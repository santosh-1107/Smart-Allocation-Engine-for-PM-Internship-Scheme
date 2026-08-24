package in.pmis.modules.policy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    @Data
    public static class IngestRequest {
        @NotBlank
        private String title;
        private String url;
        private List<Map<String, String>> chunks;
    }

    @Data
    public static class RagQueryRequest {
        @NotBlank
        private String question;
        @NotBlank
        private String role;
    }

    @PostMapping("/policy/ingest")
    public ResponseEntity<?> ingestPolicy(@Valid @RequestBody IngestRequest request) {
        PolicyDocument doc = policyService.ingestDocument(request.getTitle(), request.getUrl(), request.getChunks());
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/policy/documents")
    public ResponseEntity<List<PolicyDocument>> getDocuments() {
        return ResponseEntity.ok(policyService.getAllDocuments());
    }

    @PostMapping("/rag/query")
    public ResponseEntity<?> queryRAG(@Valid @RequestBody RagQueryRequest request) {
        Map<String, Object> response = policyService.queryPolicyRAG(request.getQuestion(), request.getRole());
        return ResponseEntity.ok(response);
    }
}
