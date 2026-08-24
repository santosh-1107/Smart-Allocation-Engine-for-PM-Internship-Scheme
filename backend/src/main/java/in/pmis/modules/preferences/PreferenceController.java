package in.pmis.modules.preferences;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    @Autowired
    private PreferenceService preferenceService;

    @Data
    public static class PreferenceSubmitRequest {
        @NotNull
        private UUID cycleId;
        @NotNull
        private List<UUID> preferenceOrder;
        private int version;
    }

    @Data
    public static class PairwiseRequest {
        @NotNull
        private UUID cycleId;
        private String action; // START or COMPARE
        private UUID chosenId;
        private UUID otherId;
    }

    @PostMapping
    public ResponseEntity<?> submitPreferences(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf,
            @Valid @RequestBody PreferenceSubmitRequest request) {

        UUID studentId;
        if ("CSC_OPERATOR".equalsIgnoreCase(userRole)) {
            if (actingOnBehalfOf == null) {
                return ResponseEntity.badRequest().body("CSC Operator must specify target student ID in X-Acting-On-Behalf-Of header");
            }
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        try {
            Preference preference = preferenceService.submitPreferences(
                    studentId,
                    request.getCycleId(),
                    request.getPreferenceOrder(),
                    request.getVersion()
            );
            return ResponseEntity.ok(preference);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "CONCURRENT_UPDATE",
                    "message", "Your preferences have been modified on another device. Please refresh and try again.",
                    "version", preferenceService.getCurrentVersion(studentId, request.getCycleId())
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf,
            @RequestParam("cycleId") UUID cycleId) {

        UUID studentId;
        if ("CSC_OPERATOR".equalsIgnoreCase(userRole)) {
            if (actingOnBehalfOf == null) {
                return ResponseEntity.badRequest().body("CSC Operator must specify target student ID in X-Acting-On-Behalf-Of");
            }
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        return preferenceService.getPreferences(studentId, cycleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pairwise")
    public ResponseEntity<?> handlePairwise(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf,
            @Valid @RequestBody PairwiseRequest request) {

        UUID studentId;
        if ("CSC_OPERATOR".equalsIgnoreCase(userRole)) {
            if (actingOnBehalfOf == null) {
                return ResponseEntity.badRequest().body("CSC Operator must specify target student ID in X-Acting-On-Behalf-Of");
            }
            studentId = UUID.fromString(actingOnBehalfOf);
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("Student ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        if ("START".equalsIgnoreCase(request.getAction())) {
            Map<String, Object> result = preferenceService.startPairwise(studentId, request.getCycleId());
            return ResponseEntity.ok(result);
        } else {
            if (request.getChosenId() == null || request.getOtherId() == null) {
                return ResponseEntity.badRequest().body("chosenId and otherId are required for COMPARE action");
            }
            Map<String, Object> result = preferenceService.processPairwise(
                    studentId,
                    request.getCycleId(),
                    request.getChosenId(),
                    request.getOtherId()
            );
            return ResponseEntity.ok(result);
        }
    }
}
