package in.pmis.modules.internships;

import in.pmis.modules.companies.Company;
import in.pmis.modules.students.Skill;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "internship_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipListing {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String location;

    private String sector;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "stipend_company_share")
    @Builder.Default
    private BigDecimal stipendCompanyShare = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "listing_requirements",
        joinColumns = @JoinColumn(name = "listing_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills;
}
