package in.pmis.modules.allocations;

import in.pmis.modules.students.Student;
import in.pmis.modules.internships.InternshipListing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "allocation_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationResult {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_run_id", nullable = false)
    private AllocationRun allocationRun;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "listing_id")
    private InternshipListing listing;

    @Column(name = "assigned_rank")
    private Integer assignedRank;

    @Column(name = "compatibility_score", precision = 8, scale = 5)
    private BigDecimal compatibilityScore;

    @Convert(converter = JsonStringConverter.class)
    @Column(columnDefinition = "jsonb")
    private String explanation;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PROPOSED";
}
