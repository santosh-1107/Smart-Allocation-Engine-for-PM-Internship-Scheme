package in.pmis.modules.companies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, UUID> {
    List<CompanyVerification> findByCompanyId(UUID companyId);
}
