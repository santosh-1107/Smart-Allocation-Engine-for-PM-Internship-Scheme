package in.pmis.modules.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, UUID> {
    List<PolicyChunk> findByDocumentId(UUID documentId);
}
