package in.pmis.modules.students;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "education")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String degree;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    private String institution;

    @Column(name = "percentage_or_cgpa", precision = 5, scale = 2)
    private BigDecimal percentageOrCgpa;

    @Column(name = "graduation_year")
    private Integer graduationYear;
}
