package com.ben.messagingplatform.infrastructure.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "messaging.events";
    private static final String ROUTING_KEY = "message.created";

    public void publish(Object event) {
        rabbitTemplate.convertAndSend(
                EXCHANGE,
                ROUTING_KEY,
                event
        );
    }

    public void publish(String eventId, String jsonPayload) {

        MessageProperties properties = new MessageProperties();

        properties.setMessageId(eventId);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message message = new Message(
                jsonPayload.getBytes(StandardCharsets.UTF_8),
                properties
        );

        CorrelationData correlationData = new CorrelationData(eventId);

        rabbitTemplate.send(
                EXCHANGE,
                ROUTING_KEY,
                message,
                correlationData
        );

        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(10, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected event: " + confirm.reason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ could not route event: " + eventId
                );
            }

        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "RabbitMQ publisher confirmation timed out for event: " + eventId,
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to publish event: " + eventId,
                    exception
            );
        }
    }
}
