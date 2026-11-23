
package com.ben.messagingplatform.routing.dto.publisher;

import com.ben.messagingplatform.infrastructure.rabbitmq.MessageRoutingConfig;
import com.ben.messagingplatform.routing.dto.MessageRoutingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class MessageRoutingPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(MessageRoutingEvent event) {

        String routingKey =
                MessageRoutingConfig.SERVER_ROUTING_KEY_PREFIX
                        + event.targetServerId();

        CorrelationData correlationData =
                new CorrelationData(event.eventId());

        rabbitTemplate.convertAndSend(
                MessageRoutingConfig.MESSAGE_ROUTING_EXCHANGE,
                routingKey,
                event,
                message -> {
                    message.getMessageProperties()
                            .setMessageId(event.eventId());

                    return message;
                },
                correlationData
        );

        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture()
                            .get(10, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected routing event: "
                                + confirm.reason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ could not route event: "
                                + event.eventId()
                );
            }

        } catch (TimeoutException exception) {

            throw new IllegalStateException(
                    "Publisher confirmation timed out for routing event: "
                            + event.eventId(),
                    exception
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while publishing routing event: "
                            + event.eventId(),
                    exception
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to publish routing event: "
                            + event.eventId(),
                    exception
            );
        }
    }
}
