package com.ben.messagingplatform.infrastructure.rabbitmq.redis;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ServerIdentity {

    private final String serverId =
            UUID.randomUUID().toString();

    public String getServerId() {
        return serverId;
    }
}
