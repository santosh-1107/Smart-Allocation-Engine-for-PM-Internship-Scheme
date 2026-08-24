package in.pmis.modules.preferences;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PreferenceVersionRepository extends JpaRepository<PreferenceVersion, UUID> {
    List<PreferenceVersion> findByStudentIdOrderByVersionDesc(UUID studentId);
}
