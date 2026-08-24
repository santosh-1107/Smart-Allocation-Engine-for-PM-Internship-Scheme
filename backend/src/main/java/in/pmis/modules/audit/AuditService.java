package in.pmis.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Transactional
    public AuditLog logEvent(String eventType, String actorId, UUID actingOnBehalfOf, Object payload) {
        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {}

        // Find the latest audit log entry to get the previous hash
        Optional<AuditLog> latestLogOpt = auditLogRepository.findFirstByOrderByCreatedAtDesc();
        String previousHash = latestLogOpt.map(AuditLog::getCurrentHash).orElse(GENESIS_HASH);

        // Build canonical entry string
        String canonicalEntry = String.format("event_type:%s|actor_id:%s|acting_on_behalf_of:%s|payload:%s",
                eventType,
                actorId != null ? actorId : "null",
                actingOnBehalfOf != null ? actingOnBehalfOf.toString() : "null",
                payloadJson
        );

        String currentHash = computeSha256(canonicalEntry + previousHash);

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .actorId(actorId)
                .actingOnBehalfOf(actingOnBehalfOf)
                .payload(payloadJson)
                .previousHash(previousHash)
                .currentHash(currentHash)
                .build();

        return auditLogRepository.save(log);
    }

    public Map<String, Object> verifyChain() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtAsc();
        Map<String, Object> result = new HashMap<>();

        String expectedPreviousHash = GENESIS_HASH;

        for (AuditLog log : logs) {
            // Check link to previous hash
            if (!log.getPreviousHash().equals(expectedPreviousHash)) {
                result.put("status", "TAMPERED");
                result.put("reason", "Chain link broken. Expected previous hash match.");
                result.put("compromisedRowId", log.getId());
                result.put("logEntry", log);
                return result;
            }

            // Verify current hash computation
            String canonicalEntry = String.format("event_type:%s|actor_id:%s|acting_on_behalf_of:%s|payload:%s",
                    log.getEventType(),
                    log.getActorId() != null ? log.getActorId() : "null",
                    log.getActingOnBehalfOf() != null ? log.getActingOnBehalfOf().toString() : "null",
                    log.getPayload()
            );

            String computedHash = computeSha256(canonicalEntry + expectedPreviousHash);
            if (!log.getCurrentHash().equals(computedHash)) {
                result.put("status", "TAMPERED");
                result.put("reason", "Hash computation mismatch. Payload or metadata altered.");
                result.put("compromisedRowId", log.getId());
                result.put("logEntry", log);
                return result;
            }

            expectedPreviousHash = log.getCurrentHash();
        }

        result.put("status", "VERIFIED");
        result.put("logCount", logs.size());
        return result;
    }

    @Transactional
    public AuditLog simulateTamper(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found"));

        // Alter payload to break the hash chain validation
        log.setPayload("{\"tempered\": true, \"alteredValue\": \"unauthorized modification\"}");
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtAsc();
    }

    private String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 digest failed", e);
        }
    }
}
