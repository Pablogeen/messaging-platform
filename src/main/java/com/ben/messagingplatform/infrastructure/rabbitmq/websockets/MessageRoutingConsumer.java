
package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnectionRegistry;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.ServerIdentity;
import com.ben.messagingplatform.message.dto.MessageResponse;
import com.ben.messagingplatform.routing.dto.MessageRoutingEvent;
import com.ben.messagingplatform.routing.dto.service.RoutingEventDeduplicationService;
import com.ben.messagingplatform.routing.dto.service.RoutingEventDeduplicationService.Claim;
import com.ben.messagingplatform.routing.dto.service.RoutingEventDeduplicationService.ClaimStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageRoutingConsumer {

    private final LocalWebSocketSessionRegistry localSessionRegistry;
    private final DeviceConnectionRegistry deviceConnectionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoutingEventDeduplicationService deduplicationService;
    private final ServerIdentity serverIdentity;

    @RabbitListener(queues = "#{messageRoutingQueue.name}")
    public void consume(MessageRoutingEvent event) {

        String serverId = serverIdentity.getServerId();

        if (!serverId.equals(event.targetServerId())) {
            log.warn(
                    "Ignoring routing event intended for server {} but received by {}",
                    event.targetServerId(),
                    serverId
            );
            return;
        }
        Claim claim = deduplicationService.claim(
                event.eventId(),
                serverId
        );

        if (claim.status() == ClaimStatus.ALREADY_PROCESSED) {
            log.info("Ignoring duplicate routing event {}", event.eventId());
            return;
        }

        if (claim.status() == ClaimStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Routing event is already being processed: " + event.eventId()
            );
        }

        try {

            List<DeviceConnection> connections =
                    localSessionRegistry.getConnectionsForUser(
                            event.recipientId()
                    );

            List<DeviceConnection> validConnections = new ArrayList<>();

            for (DeviceConnection connection : connections) {

                DeviceConnection currentConnection =
                        deviceConnectionRegistry.getConnection(
                                connection.userId(),
                                connection.deviceId()
                        );

                if (currentConnection == null ||
                        !currentConnection.generation().equals(
                                connection.generation()) ||
                        !currentConnection.sessionId().equals(
                                connection.sessionId())) {

                    localSessionRegistry.removeIfSame(
                            connection.sessionId(),
                            connection
                    );

                    continue;
                }

                validConnections.add(connection);
            }

            if (validConnections.isEmpty()) {
                boolean marked = deduplicationService.markProcessed(
                        event.eventId(), serverId, claim.token());

                if (!marked) {
                    throw new IllegalStateException(
                            "Failed to mark routing event as processed");
                }

                log.info("No active local sessions for recipient {}. Message remains available in history.",
                        event.recipientId());
                return;
            }

            MessageResponse response = new MessageResponse(
                    event.messageId(),
                    event.conversationId(),
                    event.senderId(),
                    event.sequenceNumber(),
                    event.content(),
                    event.createdAt()
            );

            for (DeviceConnection connection : validConnections) {

                SimpMessageHeaderAccessor headers =
                        SimpMessageHeaderAccessor.create();

                headers.setSessionId(connection.sessionId());
                headers.setLeaveMutable(true);

                messagingTemplate.convertAndSendToUser(
                        connection.userId().toString(),
                        "/queue/messages",
                        response,
                        headers.getMessageHeaders()
                );
            }

            boolean marked = deduplicationService.markProcessed(
                    event.eventId(),
                    serverId,
                    claim.token()
            );

            if (!marked) {
                throw new IllegalStateException(
                        "Could not finalize routing event: " + event.eventId()
                );
            }

            log.info(
                    "Delivered message {} to {} valid sessions for recipient {}",
                    event.messageId(),
                    validConnections.size(),
                    event.recipientId()
            );

        } catch (RuntimeException exception) {

            try {
                deduplicationService.release(
                        event.eventId(),
                        serverId,
                        claim.token()
                );
            } catch (RuntimeException releaseException) {
                log.error(
                        "Could not release routing claim for event {}",
                        event.eventId(),
                        releaseException
                );
            }

            throw exception;
        }
    }
}
