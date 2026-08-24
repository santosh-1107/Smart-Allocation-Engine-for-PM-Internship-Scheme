package in.pmis.modules.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getLogs() {
        return ResponseEntity.ok(auditService.getLogs());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        Map<String, Object> verificationResult = auditService.verifyChain();
        return ResponseEntity.ok(verificationResult);
    }

    @PostMapping("/tamper")
    public ResponseEntity<?> simulateTamper(@RequestParam("id") UUID id) {
        AuditLog tamperedLog = auditService.simulateTamper(id);
        return ResponseEntity.ok(Map.of(
                "status", "TAMPERED_IN_DATABASE",
                "message", "Audit log has been modified directly in the database. Run verify endpoint to detect.",
                "tamperedEntry", tamperedLog
        ));
    }
}
