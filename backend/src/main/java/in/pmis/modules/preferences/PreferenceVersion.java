package in.pmis.modules.preferences;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "preference_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceVersion {
    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private Integer version;

    @Convert(converter = ListUuidConverter.class)
    @Column(name = "preference_order", nullable = false, columnDefinition = "jsonb")
    private List<UUID> preferenceOrder;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
