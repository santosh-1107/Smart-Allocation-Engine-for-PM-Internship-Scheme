package in.pmis.modules.students;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<?> getStudents() {
        // Return first 50 students to keep the frontend dropdown clean and responsive
        return ResponseEntity.ok(studentRepository.findAll().stream().limit(50).collect(java.util.stream.Collectors.toList()));
    }

    @Data
    public static class ProfileRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;
        private String phone;
        private String preferredLanguage;
        private String district;
        private boolean aspirationalDistrict;
        private String category;
        private String gender;
        private String dob;
        private boolean failEkyc;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> createOrUpdateProfile(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Acting-On-Behalf-Of", required = false) String actingOnBehalfOf,
            @Valid @RequestBody ProfileRequest request) {

        UUID studentId;
        String operatorId = null;

        if ("CSC_OPERATOR".equalsIgnoreCase(userRole)) {
            if (actingOnBehalfOf == null) {
                return ResponseEntity.badRequest().body("CSC Operator must specify target student ID in X-Acting-On-Behalf-Of header");
            }
            studentId = UUID.fromString(actingOnBehalfOf);
            operatorId = userId;
        } else {
            if (userId == null) {
                return ResponseEntity.badRequest().body("User ID must be provided in X-User-Id header");
            }
            studentId = UUID.fromString(userId);
        }

        StudentProfile profile = studentService.createOrUpdateProfile(
                studentId,
                request.getFullName(),
                request.getPhone(),
                request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "en",
                request.getDistrict(),
                request.isAspirationalDistrict(),
                request.getCategory() != null ? request.getCategory() : "GENERAL",
                request.getGender(),
                request.getDob(),
                request.isFailEkyc(),
                operatorId
        );

        return ResponseEntity.ok(profile);
    }
}
