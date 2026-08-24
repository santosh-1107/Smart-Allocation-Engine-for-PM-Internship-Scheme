package in.pmis.modules.notifications;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    public static final String QUEUE_NAME = "notifications.queue";
    public static final String DLQ_NAME = "notifications.dlq";
    public static final String EXCHANGE_NAME = "notifications.exchange";
    public static final String DLX_NAME = "notifications.dlx";
    public static final String ROUTING_KEY = "notifications.route";
    public static final String DLQ_ROUTING_KEY = "notifications.dlq.route";

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Binding bindingQueue(Queue notificationsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationsExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding bindingDLQ(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }
}
