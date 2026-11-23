
package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnectionRegistry;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.ServerIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionEventListener {

    private final ServerIdentity serverIdentity;
    private final DeviceConnectionRegistry deviceConnectionRegistry;
    private final LocalWebSocketSessionRegistry localSessionRegistry;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();
        Map<String, Object> attributes = accessor.getSessionAttributes();

        if (sessionId == null || principal == null || attributes == null) {
            return;
        }

        String deviceId = (String) attributes.get("device-id");

        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());

        Long generation = deviceConnectionRegistry.registerConnection(
                userId,
                deviceId,
                sessionId,
                serverIdentity.getServerId()
        );

        DeviceConnection connection = new DeviceConnection(
                userId,
                deviceId,
                sessionId,
                serverIdentity.getServerId(),
                generation
        );

        localSessionRegistry.register(connection);

        log.info(
                "Registered WebSocket session {} for user {} on server {}",
                sessionId,
                userId,
                serverIdentity.getServerId()
        );
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        DeviceConnection connection =
                localSessionRegistry.remove(sessionId);

        if (connection == null) {
            return;
        }

        deviceConnectionRegistry.removeConnection(
                connection.userId(),
                connection.deviceId(),
                connection.generation()
        );

        log.info(
                "Removed WebSocket session {} for user {}",
                sessionId,
                connection.userId()
        );
    }

    @Scheduled(fixedRate = 300000)
    public void refreshActiveSessions() {

        localSessionRegistry.getAllConnections().forEach(connection -> {

            boolean refreshed = deviceConnectionRegistry.refreshConnection(
                    connection.userId(),
                    connection.deviceId(),
                    connection.generation()
            );

            if (!refreshed) {
                localSessionRegistry.removeIfSame(
                        connection.sessionId(),
                        connection
                );

                log.warn(
                        "Removed stale local session {} for user {}",
                        connection.sessionId(),
                        connection.userId()
                );
            }
        });
    }
}
