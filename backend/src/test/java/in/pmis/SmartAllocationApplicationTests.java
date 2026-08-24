package in.pmis;

import in.pmis.modules.audit.AuditLog;
import in.pmis.modules.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class SmartAllocationApplicationTests {

    @Autowired
    private AuditService auditService;

    @Test
    void contextLoads() {
    }

    @Test
    void testAuditHashChainAndTamperDetection() {
        int initialSize = auditService.getLogs().size();

        // Log some events
        AuditLog log1 = auditService.logEvent("TEST_EVENT_1", "actor-1", null, Map.of("data", "value1"));
        AuditLog log2 = auditService.logEvent("TEST_EVENT_2", "actor-2", null, Map.of("data", "value2"));

        assertNotNull(log1);
        assertNotNull(log2);
        assertEquals(log1.getCurrentHash(), log2.getPreviousHash());

        // Verify chain integrity is secure
        Map<String, Object> verifyResult = auditService.verifyChain();
        assertEquals("VERIFIED", verifyResult.get("status"));

        // Simulate tampering in the database
        auditService.simulateTamper(log1.getId());

        // Re-verify, should detect tamper
        Map<String, Object> verifyResultAfterTamper = auditService.verifyChain();
        assertEquals("TAMPERED", verifyResultAfterTamper.get("status"));
        assertEquals(log1.getId(), verifyResultAfterTamper.get("compromisedRowId"));
    }
}
