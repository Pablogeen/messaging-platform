
package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.MessageCreatedEventDeduplicationService;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.MessageCreatedEventDeduplicationService.Claim;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.MessageCreatedEventDeduplicationService.ClaimStatus;
import com.ben.messagingplatform.message.event.MessageCreatedEvent;
import com.ben.messagingplatform.routing.dto.service.MessageRoutingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageCreatedEventConsumer {

    private final MessageRoutingOrchestrator routingOrchestrator;
    private final MessageCreatedEventDeduplicationService deduplicationService;

    @RabbitListener(queues = "message.created.queue")
    public void consume(
            MessageCreatedEvent event,
            @Header(AmqpHeaders.MESSAGE_ID) String eventId
    ) {

        Claim claim = deduplicationService.claim(eventId);

        if (claim.status() == ClaimStatus.ALREADY_PROCESSED) {
            log.info("Duplicate message-created event ignored: {}", eventId);
            return;
        }

        if (claim.status() == ClaimStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Message-created event is already being processed: " + eventId
            );
        }

        try {

            log.info("Processing message-created event: {}", event.messageId());

            routingOrchestrator.routeMessage(event, eventId);

            boolean marked = deduplicationService.markProcessed(
                    eventId,
                    claim.token()
            );

            if (!marked) {
                throw new IllegalStateException(
                        "Could not finalize message-created event: " + eventId
                );
            }

            log.info("Successfully processed message-created event: {}", eventId);

        } catch (RuntimeException exception) {

            try {
                deduplicationService.release(eventId, claim.token());
            } catch (RuntimeException releaseException) {
                log.error(
                        "Could not release processing claim for event: {}",
                        eventId,
                        releaseException
                );
            }

            throw exception;
        }
    }
}
