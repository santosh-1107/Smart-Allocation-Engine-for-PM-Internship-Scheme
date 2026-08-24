package in.pmis.modules.companies;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyVerification {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "auditor_id")
    private String auditorId;

    @Column(nullable = false)
    private String status;

    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
