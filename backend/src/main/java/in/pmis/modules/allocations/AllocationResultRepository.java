package in.pmis.modules.allocations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllocationResultRepository extends JpaRepository<AllocationResult, UUID> {
    List<AllocationResult> findByAllocationRunId(UUID runId);
    Optional<AllocationResult> findByAllocationRunIdAndStudentId(UUID runId, UUID studentId);
    Optional<AllocationResult> findByStudentIdAndStatus(UUID studentId, String status);
    List<AllocationResult> findByListingId(UUID listingId);
    List<AllocationResult> findByListingIdAndStatus(UUID listingId, String status);
}
