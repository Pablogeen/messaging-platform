package com.ben.messagingplatform.routing.dto.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RoutingEventDeduplicationService {

    private static final Duration EVENT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public boolean markIfNew(String eventId, String serverId) {

        String key = "routing:processed:" + serverId + ":" + eventId;

        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSED", EVENT_TTL);

        return Boolean.TRUE.equals(result);
    }
}

