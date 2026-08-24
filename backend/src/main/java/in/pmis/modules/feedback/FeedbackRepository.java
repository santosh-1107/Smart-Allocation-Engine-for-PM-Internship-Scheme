package in.pmis.modules.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByStudentId(UUID studentId);
    List<Feedback> findByCompanyId(UUID companyId);
}
