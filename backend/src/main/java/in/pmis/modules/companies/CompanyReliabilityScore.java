package in.pmis.modules.companies;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "company_reliability_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReliabilityScore {
    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "onboarding_count")
    @Builder.Default
    private Integer onboardingCount = 0;

    @Column(name = "withdrawal_count")
    @Builder.Default
    private Integer withdrawalCount = 0;

    @Column(name = "reliability_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal reliabilityScore = BigDecimal.valueOf(100.0);
}
