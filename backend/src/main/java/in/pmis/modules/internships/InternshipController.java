package in.pmis.modules.internships;

import in.pmis.modules.companies.JoiningConfirmation;
import in.pmis.modules.allocations.AllocationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internships")
public class InternshipController {

    @Autowired
    private InternshipService internshipService;

    @Data
    public static class CreateListingRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String location;
        private String sector;
        @Min(1)
        private int capacity;
        @NotNull
        private BigDecimal stipendCompanyShare;
        private List<String> requiredSkills;
    }

    @Data
    public static class UpdateListingRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String location;
        private String sector;
        private List<String> requiredSkills;
    }

    @Data
    public static class CapacityRequest {
        @Min(0)
        private int capacity;
    }

    @Data
    public static class OnboardingRequest {
        private UUID studentId;
        private boolean confirmed;
        private String comments;
    }

    @GetMapping
    public ResponseEntity<List<InternshipListing>> getListings(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        if (all) {
            return ResponseEntity.ok(internshipService.getAllListings());
        } else {
            return ResponseEntity.ok(internshipService.getActiveListings());
        }
    }

    @PostMapping
    public ResponseEntity<?> createListing(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateListingRequest request) {

        if (!"COMPANY_RECRUITER".equalsIgnoreCase(userRole) && !"NATIONAL_ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(430).body("Access denied. Recruiter role required.");
        }

        UUID companyId;
        if (userId == null) {
            return ResponseEntity.badRequest().body("Company ID must be provided in X-User-Id header");
        }
        companyId = UUID.fromString(userId);

        InternshipListing listing = internshipService.createListing(
                companyId,
                request.getTitle(),
                request.getLocation(),
                request.getSector(),
                request.getCapacity(),
                request.getStipendCompanyShare(),
                request.getRequiredSkills()
        );

        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateListingRequest request) {

        InternshipListing listing = internshipService.updateListing(
                id,
                request.getTitle(),
                request.getLocation(),
                request.getSector(),
                request.getRequiredSkills()
        );

        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}/capacity")
    public ResponseEntity<?> updateCapacity(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CapacityRequest request) {

        InternshipListing listing = internshipService.updateCapacity(id, request.getCapacity());
        return ResponseEntity.ok(listing);
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<List<AllocationResult>> getApplications(@PathVariable("id") UUID id) {
        List<AllocationResult> applications = internshipService.getApplications(id);
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/{id}/joining-confirmation")
    public ResponseEntity<?> confirmJoining(
            @PathVariable("id") UUID id,
            @Valid @RequestBody OnboardingRequest request) {

        JoiningConfirmation confirmation = internshipService.confirmJoining(
                id,
                request.getStudentId(),
                request.isConfirmed(),
                request.getComments()
        );

        return ResponseEntity.ok(confirmation);
    }
}
