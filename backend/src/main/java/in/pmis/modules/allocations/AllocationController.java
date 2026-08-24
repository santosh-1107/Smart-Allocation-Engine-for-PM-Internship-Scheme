package in.pmis.modules.allocations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/allocation")
public class AllocationController {

    @Autowired
    private AllocationService allocationService;

    @Data
    public static class SimulateRequest {
        @NotNull
        private UUID cycleId;
        @NotNull
        private BigDecimal budgetCeiling;
        private long seed = 42L;
    }

    @Data
    public static class ApproveRequest {
        @NotNull(message = "Justification is mandatory")
        private String justification;
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulateAllocation(@Valid @RequestBody SimulateRequest request) {
        Map<String, Object> result = allocationService.simulateOrRunAllocation(
                request.getCycleId(),
                request.getBudgetCeiling(),
                request.getSeed(),
                false // commitToApprovalQueue = false (Draft run)
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run")
    public ResponseEntity<?> runAllocation(@Valid @RequestBody SimulateRequest request) {
        Map<String, Object> result = allocationService.simulateOrRunAllocation(
                request.getCycleId(),
                request.getBudgetCeiling(),
                request.getSeed(),
                true // commitToApprovalQueue = true (Ready for approval)
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{runId}/approve")
    public ResponseEntity<?> approveRun(
            @PathVariable("runId") UUID runId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ApproveRequest request) {

        String actorId = userId != null ? userId : "SYSTEM_ADMIN";
        allocationService.approveRun(runId, request.getJustification(), actorId);
        return ResponseEntity.ok(Map.of(
                "status", "RUN_APPROVED_AND_COMMITTED",
                "message", "Run approved. Proposed outcomes are published, and notifications are sent."
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf) {

        UUID studentId;
        if (actingOnBehalfOf != null) {
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        Map<String, Object> explanation = allocationService.getStudentExplanation(studentId);
        return ResponseEntity.ok(explanation);
    }

    @GetMapping("/explanation")
    public ResponseEntity<?> getExplanation(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf) {

        UUID studentId;
        if (actingOnBehalfOf != null) {
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        Map<String, Object> explanation = allocationService.getStudentExplanation(studentId);
        return ResponseEntity.ok(explanation);
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptAllocation(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf) {

        UUID studentId;
        if (actingOnBehalfOf != null) {
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        allocationService.acceptAllocation(studentId);
        return ResponseEntity.ok(Map.of("status", "ACCEPTED", "message", "Allocation accepted successfully."));
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectAllocation(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf) {

        UUID studentId;
        if (actingOnBehalfOf != null) {
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        allocationService.rejectAllocation(studentId);
        return ResponseEntity.ok(Map.of("status", "REJECTED", "message", "Allocation rejected. Waitlist reallocation triggered."));
    }

    @GetMapping("/counterfactual")
    public ResponseEntity<?> getCounterfactual(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf,
            @RequestParam("field") String field,
            @RequestParam("value") String value) {

        UUID studentId;
        if (actingOnBehalfOf != null) {
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        Map<String, Object> result = allocationService.queryCounterfactual(studentId, field, value);
        return ResponseEntity.ok(result);
    }
}
