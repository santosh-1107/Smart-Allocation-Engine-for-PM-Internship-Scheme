package in.pmis.modules.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {
}
