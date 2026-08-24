package in.pmis.modules.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAction {
    @Id
    private UUID id;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "target_id")
    private String targetId;

    @Column(nullable = false)
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
