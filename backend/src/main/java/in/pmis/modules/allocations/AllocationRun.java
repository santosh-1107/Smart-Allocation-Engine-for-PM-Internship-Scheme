package in.pmis.modules.allocations;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "allocation_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRun {
    @Id
    private UUID id;

    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Long seed;

    @Column(name = "budget_ceiling", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetCeiling;

    @Convert(converter = JsonStringConverter.class)
    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    private String inputSnapshot;

    @Convert(converter = JsonStringConverter.class)
    @Column(columnDefinition = "jsonb")
    private String metrics;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
