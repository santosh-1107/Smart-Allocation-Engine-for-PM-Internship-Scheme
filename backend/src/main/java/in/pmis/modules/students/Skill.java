package in.pmis.modules.students;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;
}
