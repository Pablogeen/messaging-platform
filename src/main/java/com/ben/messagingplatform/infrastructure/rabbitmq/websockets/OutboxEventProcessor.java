package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import com.ben.messagingplatform.infrastructure.rabbitmq.RabbitMQPublisher;
import com.ben.messagingplatform.outbox.entity.OutboxEvent;
import com.ben.messagingplatform.outbox.repository.OutboxEventRepository;
import com.ben.messagingplatform.outbox.service.OutboxEventClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventClaimService outboxEventClaimService;
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitMQPublisher rabbitMQPublisher;

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {

        List<OutboxEvent> events =
                outboxEventClaimService.claimPendingEvents();

        for (OutboxEvent event : events) {
            try {
                rabbitMQPublisher.publish(
                        event.getId().toString(),
                        event.getPayload());

                event.markPublished();

                outboxEventRepository.save(event);

                log.info("Successfully published outbox event: {}", event.getId());

            } catch (Exception exception) {

                event.incrementRetryCount();

                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.markFailed();
                } else {
                    event.markPending();
                }

                outboxEventRepository.save(event);

                log.error(
                        "Failed to publish outbox event: {}. Retry count: {}",
                        event.getId(),
                        event.getRetryCount(),
                        exception
                );
            }
        }
    }
}