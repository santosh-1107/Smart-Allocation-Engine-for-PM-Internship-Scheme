package in.pmis.modules.companies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JoiningConfirmationRepository extends JpaRepository<JoiningConfirmation, UUID> {
    Optional<JoiningConfirmation> findByStudentIdAndListingId(UUID studentId, UUID listingId);
}
