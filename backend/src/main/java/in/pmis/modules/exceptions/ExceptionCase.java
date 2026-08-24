package in.pmis.modules.exceptions;

import in.pmis.modules.allocations.JsonStringConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exception_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionCase {
    @Id
    private UUID id;

    @Column(name = "case_type", nullable = false)
    private String caseType;

    @Column(nullable = false)
    private String severity;

    @Column(name = "entity_id")
    private UUID entityId;

    @Convert(converter = JsonStringConverter.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String context;

    @Column(nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "resolution_reason")
    private String resolutionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
