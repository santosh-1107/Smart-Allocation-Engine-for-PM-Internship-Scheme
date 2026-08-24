package in.pmis.modules.notifications;

public interface SmsAdapter {
    void sendSms(String recipientPhone, String message);
}
