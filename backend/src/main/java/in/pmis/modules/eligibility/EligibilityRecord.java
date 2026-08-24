package in.pmis.modules.eligibility;

import in.pmis.modules.students.Student;
import in.pmis.modules.internships.InternshipListing;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eligibility_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRecord {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private InternshipListing listing;

    @Builder.Default
    private Boolean eligible = true;

    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
