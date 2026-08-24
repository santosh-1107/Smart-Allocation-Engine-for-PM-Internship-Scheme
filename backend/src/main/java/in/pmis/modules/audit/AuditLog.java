package in.pmis.modules.audit;

import in.pmis.modules.allocations.JsonStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "acting_on_behalf_of")
    private UUID actingOnBehalfOf;

    @Convert(converter = JsonStringConverter.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "previous_hash", length = 128)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length = 128)
    private String currentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
