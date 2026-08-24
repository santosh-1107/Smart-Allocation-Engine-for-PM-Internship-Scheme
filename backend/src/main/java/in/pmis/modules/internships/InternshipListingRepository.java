package in.pmis.modules.internships;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InternshipListingRepository extends JpaRepository<InternshipListing, UUID> {
    List<InternshipListing> findByCompanyId(UUID companyId);
    List<InternshipListing> findByStatus(String status);
}
