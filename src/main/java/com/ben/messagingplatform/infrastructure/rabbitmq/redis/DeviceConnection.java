package com.ben.messagingplatform.infrastructure.rabbitmq.redis;


public record DeviceConnection(
        Long userId,
        String deviceId,
        String sessionId,
        String serverId,
        Long generation
) {}
