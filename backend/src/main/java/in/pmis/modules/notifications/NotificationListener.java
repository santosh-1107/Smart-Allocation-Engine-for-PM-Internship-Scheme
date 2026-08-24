package in.pmis.modules.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SmsAdapter smsAdapter;

    @RabbitListener(queues = NotificationConfig.QUEUE_NAME)
    public void processNotification(Map<String, Object> message) {
        logger.info("[NOTIFICATIONS LISTENER] Received message: {}", message);

        String idStr = (String) message.get("id");
        UUID id = UUID.fromString(idStr);
        String recipientId = (String) message.get("recipientId");
        String channel = (String) message.get("channel");
        String messageBody = (String) message.get("messageBody");

        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isEmpty()) {
            return;
        }
        Notification notification = notificationOpt.get();

        try {
            // Simulate critical channel retry / dead-letter behavior:
            // If the recipientId starts with "9999" (a mock number), we throw a simulated connection failure!
            if (recipientId != null && recipientId.startsWith("9999")) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus("RETRIES_EXHAUSTED_IN_DB_OR_RETRYING");
                notificationRepository.save(notification);
                logger.error("[SMS GATEWAY] Connection failed. Simulating message retry for: {}", recipientId);
                throw new RuntimeException("SMS Gateway connection timed out");
            }

            if ("SMS".equalsIgnoreCase(channel)) {
                smsAdapter.sendSms(recipientId, messageBody);
            } else {
                logger.info("[NOTIFICATION GATEWAY] Sending to IN_APP / EMAIL: {} -> {}", recipientId, messageBody);
            }

            notification.setStatus("SENT");
            notificationRepository.save(notification);
            logger.info("[NOTIFICATIONS LISTENER] Notification sent successfully for: {}", recipientId);

        } catch (Exception e) {
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
            // Re-throw so RabbitMQ can trigger standard retry logic and DLQ routing
            throw e;
        }
    }

    @RabbitListener(queues = NotificationConfig.DLQ_NAME)
    public void processDeadLetter(Map<String, Object> message) {
        logger.error("[DEAD LETTER QUEUE] Notification permanently failed and routed to DLQ! Message: {}", message);
        String idStr = (String) message.get("id");
        UUID id = UUID.fromString(idStr);
        
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setStatus("DEAD_LETTER");
            notification.setErrorMessage("Routed to Dead Letter Queue (DLQ)");
            notificationRepository.save(notification);
        }
    }
}
