package in.pmis.modules.eligibility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityRecordRepository extends JpaRepository<EligibilityRecord, UUID> {
    List<EligibilityRecord> findByStudentId(UUID studentId);
    Optional<EligibilityRecord> findByStudentIdAndListingId(UUID studentId, UUID listingId);
}
