package in.pmis.modules.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleSmsAdapter implements SmsAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleSmsAdapter.class);

    @Override
    public void sendSms(String recipientPhone, String message) {
        logger.info("[SMS GATEWAY] Sending SMS to: {} -> Message: \"{}\"", recipientPhone, message);
    }
}
