package com.ben.messagingplatform.routing.dto.service;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import com.ben.messagingplatform.message.event.MessageCreatedEvent;
import com.ben.messagingplatform.message.service.MessageRecipientService;
import com.ben.messagingplatform.routing.dto.MessageRoutingEvent;
import com.ben.messagingplatform.routing.dto.publisher.MessageRoutingPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRoutingOrchestrator {

    private final MessageRecipientService messageRecipientService;
    private final MessageRoutingService messageRoutingService;
    private final MessageRoutingPublisher messageRoutingPublisher;

    public void routeMessage(MessageCreatedEvent event, String eventId) {

        List<Long> recipientIds =
                messageRecipientService.getRecipientIds(
                        event.conversationId(),
                        event.senderId()
                );

        if (recipientIds.isEmpty()) {
            log.info("No recipients found for message {}", event.messageId());
            return;
        }

        for (Long recipientId : recipientIds) {

            Map<String, List<DeviceConnection>> serverConnections =
                    messageRoutingService.getRecipientServers(recipientId);

            for (Map.Entry<String, List<DeviceConnection>> entry
                    : serverConnections.entrySet()) {

                String serverId = entry.getKey();

                MessageRoutingEvent routingEvent =
                        new MessageRoutingEvent(
                                eventId + ":" + recipientId + ":" + serverId,
                                recipientId,
                                event.conversationId(),
                                event.messageId(),
                                event.senderId(),
                                event.content(),
                                event.sequenceNumber(),
                                event.createdAt(),
                                serverId
                        );

                messageRoutingPublisher.publish(routingEvent);

                log.info(
                        "Routed message {} for recipient {} to server {}",
                        event.messageId(),
                        recipientId,
                        serverId
                );
            }
        }
    }
}

