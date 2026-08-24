package in.pmis.modules.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class PolicyService {

    @Autowired
    private PolicyDocumentRepository policyDocumentRepository;

    @Autowired
    private PolicyChunkRepository policyChunkRepository;

    @Value("${services.rag-service:http://localhost:8003}")
    private String ragServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public PolicyDocument ingestDocument(String title, String url, List<Map<String, String>> chunksList) {
        PolicyDocument doc = PolicyDocument.builder()
                .id(UUID.randomUUID())
                .title(title)
                .url(url)
                .build();
        policyDocumentRepository.save(doc);

        if (chunksList != null) {
            for (Map<String, String> chunkMap : chunksList) {
                PolicyChunk chunk = PolicyChunk.builder()
                        .id(UUID.randomUUID())
                        .document(doc)
                        .sectionTitle(chunkMap.get("sectionTitle"))
                        .content(chunkMap.get("content"))
                        .build();
                policyChunkRepository.save(chunk);
            }
        }

        return doc;
    }

    public List<PolicyDocument> getAllDocuments() {
        return policyDocumentRepository.findAll();
    }

    public Map<String, Object> queryPolicyRAG(String question, String role) {
        String endpoint = ragServiceUrl + "/v1/query";
        Map<String, String> request = new HashMap<>();
        request.put("question", question);
        request.put("role", role);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("answer", "Policy search service is currently unavailable. Please contact support.");
            fallback.put("confidence", 0.0);
            fallback.put("sources", Collections.emptyList());
            fallback.put("requires_human_review", true);
            return fallback;
        }
    }
}
