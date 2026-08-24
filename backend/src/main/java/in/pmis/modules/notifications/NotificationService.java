package in.pmis.modules.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Vernacular template mappings
    private static final Map<String, Map<String, String>> TEMPLATES = new HashMap<>();

    static {
        Map<String, String> allocTemplates = new HashMap<>();
        allocTemplates.put("en", "Dear %s, you have been proposed for the internship %s at %s. Please login to accept or reject.");
        allocTemplates.put("hi", "प्रिय %s, आपको %s में इंटर्नशिप %s के लिए चुना गया है। कृपया स्वीकार या अस्वीकार करने के लिए लॉग इन करें।");
        allocTemplates.put("mr", "प्रिय %s, तुमची %s मधील %s इंटर्नशिपसाठी निवड झाली आहे. कृपया स्वीकार किंवा नाकार करण्यासाठी लॉग इन करा.");
        TEMPLATES.put("ALLOCATION_PROPOSED", allocTemplates);
    }

    @Transactional
    public Notification sendNotification(String recipientId, String channel, String templateName,
                                         String language, Map<String, String> variables) {
        
        String cleanLanguage = (language == null) ? "en" : language.toLowerCase();
        Map<String, String> langTemplates = TEMPLATES.get(templateName);
        
        String messageBody;
        if (langTemplates != null && langTemplates.containsKey(cleanLanguage)) {
            String rawTemplate = langTemplates.get(cleanLanguage);
            messageBody = String.format(rawTemplate, 
                    variables.getOrDefault("studentName", "Student"),
                    variables.getOrDefault("listingTitle", "Internship"),
                    variables.getOrDefault("companyName", "Company")
            );
        } else {
            messageBody = String.format("Notification event: %s for %s", templateName, recipientId);
        }

        String payloadJson = "{}";
        try {
            Map<String, Object> payloadMap = new HashMap<>(variables);
            payloadMap.put("messageBody", messageBody);
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception ignored) {}

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(recipientId)
                .channel(channel)
                .templateName(templateName)
                .language(cleanLanguage)
                .payload(payloadJson)
                .status("PENDING")
                .build();

        notificationRepository.save(notification);

        // Publish to RabbitMQ
        Map<String, Object> message = new HashMap<>();
        message.put("id", notification.getId().toString());
        message.put("recipientId", recipientId);
        message.put("channel", channel);
        message.put("messageBody", messageBody);

        try {
            rabbitTemplate.convertAndSend(
                    NotificationConfig.EXCHANGE_NAME,
                    NotificationConfig.ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage("RabbitMQ delivery failed: " + e.getMessage());
            notificationRepository.save(notification);
        }

        return notification;
    }
}
