package com.ben.messagingplatform.routing.dto.service;

import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnection;
import com.ben.messagingplatform.infrastructure.rabbitmq.redis.DeviceConnectionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageRoutingService {

    private final DeviceConnectionRegistry deviceConnectionRegistry;

    public Map<String, List<DeviceConnection>> getRecipientServers(
            Long recipientId
    ) {

        List<DeviceConnection> connections =
                deviceConnectionRegistry.getActiveConnections(recipientId);

        return connections.stream()
                .collect(Collectors.groupingBy(
                        DeviceConnection::serverId
                ));
    }
}
