package com.ben.messagingplatform.infrastructure.rabbitmq.redis;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisConnectionRegistry {

    private final StringRedisTemplate redisTemplate;

    private static final Duration SESSION_TTL =
            Duration.ofMinutes(30);

    public void registerConnection(Long userId, String sessionId, String serverId) {
        String userConnectionsKey =
                "user:" + userId + ":connections";

        String sessionKey =
                "session:" + sessionId;

        redisTemplate.opsForSet()
                .add(userConnectionsKey, sessionId);

        redisTemplate.opsForHash().putAll(
                sessionKey,
                java.util.Map.of(
                        "userId", userId.toString(),
                        "sessionId", sessionId,
                        "serverId", serverId
                )
        );

        redisTemplate.expire(sessionKey, SESSION_TTL);
    }

    public Set<String> getUserConnections(Long userId) {

        String userConnectionsKey =
                "user:" + userId + ":connections";

        Set<String> sessionIds =
                redisTemplate.opsForSet()
                        .members(userConnectionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }

        Set<String> activeSessions = new java.util.HashSet<>();

        for (String sessionId : sessionIds) {

            Boolean exists = redisTemplate.hasKey(
                    "session:" + sessionId
            );

            if (Boolean.TRUE.equals(exists)) {
                activeSessions.add(sessionId);
            } else {
                redisTemplate.opsForSet()
                        .remove(userConnectionsKey, sessionId);
            }
        }

        return activeSessions;
    }

    public void removeConnection(Long userId, String sessionId) {

        redisTemplate.opsForSet()
                .remove(
                        "user:" + userId + ":connections",
                        sessionId
                );

        redisTemplate.delete("session:" + sessionId);
    }

    public boolean refreshConnection(String sessionId) {
        Boolean refreshed = redisTemplate.expire(
                "session:" + sessionId,
                SESSION_TTL
        );

        return Boolean.TRUE.equals(refreshed);
    }
}
