package in.pmis.modules.allocations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllocationRunRepository extends JpaRepository<AllocationRun, UUID> {
    List<AllocationRun> findByCycleId(UUID cycleId);
    Optional<AllocationRun> findFirstByCycleIdOrderByCreatedAtDesc(UUID cycleId);
}
