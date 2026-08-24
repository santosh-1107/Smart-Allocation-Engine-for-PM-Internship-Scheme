package in.pmis.modules.exceptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExceptionCaseRepository extends JpaRepository<ExceptionCase, UUID> {
    List<ExceptionCase> findByStatus(String status);
}
