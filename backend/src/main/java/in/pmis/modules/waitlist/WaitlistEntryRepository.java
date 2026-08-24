package in.pmis.modules.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
    List<WaitlistEntry> findByListingIdOrderByRankPositionAsc(UUID listingId);
    List<WaitlistEntry> findByListingIdAndStatusOrderByRankPositionAsc(UUID listingId, String status);
}
