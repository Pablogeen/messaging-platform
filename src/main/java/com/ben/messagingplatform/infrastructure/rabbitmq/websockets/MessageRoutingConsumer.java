package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;


import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnectionRegistry;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.ServerIdentity;
import com.ben.messagingplatform.message.dto.MessageResponse;
import com.ben.messagingplatform.routing.dto.MessageRoutingEvent;
import com.ben.messagingplatform.routing.dto.service.RoutingEventDeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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

        var connections = localSessionRegistry
                .getConnectionsForUser(event.recipientId());

        var validConnections = new java.util.ArrayList<DeviceConnection>();

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
            log.debug(
                    "No valid local sessions for recipient {}",
                    event.recipientId()
            );
            return;
        }

        boolean isNewEvent = deduplicationService.markIfNew(
                event.eventId(),
                serverIdentity.getServerId()
        );

        if (!isNewEvent) {
            log.info("Ignoring duplicate routing event {}", event.eventId());
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

        log.info(
                "Delivered message {} to {} valid sessions for recipient {}",
                event.messageId(),
                validConnections.size(),
                event.recipientId()
        );
    }



}

