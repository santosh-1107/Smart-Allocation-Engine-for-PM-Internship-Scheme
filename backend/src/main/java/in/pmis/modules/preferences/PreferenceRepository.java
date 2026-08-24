package in.pmis.modules.preferences;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, UUID> {
    Optional<Preference> findByStudentIdAndCycleId(UUID studentId, UUID cycleId);
    List<Preference> findByCycleId(UUID cycleId);

    @Query("SELECT COALESCE(MAX(p.version), 0) FROM Preference p WHERE p.student.id = :studentId AND p.cycleId = :cycleId")
    int findMaxVersionByStudentIdAndCycleId(UUID studentId, UUID cycleId);
}
