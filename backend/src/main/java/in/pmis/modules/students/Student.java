package in.pmis.modules.students;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "en";

    private String district;

    @Column(name = "aspirational_district")
    @Builder.Default
    private Boolean aspirationalDistrict = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "student_skills",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills;
}
