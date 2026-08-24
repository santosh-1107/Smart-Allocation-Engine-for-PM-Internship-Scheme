package in.pmis.modules.allocations;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reallocation_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReallocationEvent {
    @Id
    private UUID id;

    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "previous_listing_id")
    private UUID previousListingId;

    @Column(name = "new_listing_id")
    private UUID newListingId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
