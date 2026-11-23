package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;



import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalWebSocketSessionRegistry {

    private final Map<String, DeviceConnection> localConnections =
            new ConcurrentHashMap<>();

    public void register(DeviceConnection connection) {
        localConnections.put(connection.sessionId(), connection);
    }

    public DeviceConnection remove(String sessionId) {
        return localConnections.remove(sessionId);
    }

    public List<DeviceConnection> getConnectionsForUser(Long userId) {
        return localConnections.values()
                .stream()
                .filter(connection -> connection.userId().equals(userId))
                .toList();
    }

    public List<DeviceConnection> getAllConnections() {
        return List.copyOf(localConnections.values());
    }

    public void removeIfSame(String sessionId, DeviceConnection connection) {
        localConnections.remove(sessionId, connection);
    }
}

