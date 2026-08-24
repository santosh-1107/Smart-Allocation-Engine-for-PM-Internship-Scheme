package in.pmis.modules.students;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {
    @Id
    @Column(name = "student_id")
    private UUID studentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "student_id")
    private Student student;

    @Builder.Default
    private String category = "GENERAL";

    private String gender;

    private LocalDate dob;

    @Column(name = "ekyc_verified")
    @Builder.Default
    private Boolean ekycVerified = false;

    @Column(name = "ekyc_failed_reason")
    private String ekycFailedReason;

    @Column(name = "acting_on_behalf_of")
    private String actingOnBehalfOf;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
