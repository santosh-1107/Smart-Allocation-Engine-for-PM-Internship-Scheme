package in.pmis.modules.companies;

import in.pmis.modules.students.Student;
import in.pmis.modules.internships.InternshipListing;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "joining_confirmations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoiningConfirmation {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private InternshipListing listing;

    @Column(nullable = false)
    @Builder.Default
    private Boolean confirmed = false;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "confirmed_at", nullable = false)
    @Builder.Default
    private Instant confirmedAt = Instant.now();
}
