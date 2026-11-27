
package com.ben.messagingplatform.infrastructure.rabbitmq.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageCreatedEventDeduplicationService {

    private static final Duration PROCESSING_TTL =
            Duration.ofMinutes(5);

    private static final Duration PROCESSED_TTL =
            Duration.ofHours(24);

    private static final String PROCESSED = "PROCESSED";

    private static final DefaultRedisScript<Long> MARK_PROCESSED_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                        return 1
                    end
                    return 0
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public Claim claim(String eventId) {

        String key = buildKey(eventId);
        String token = UUID.randomUUID().toString();
        String processingValue = "PROCESSING:" + token;

        String currentValue = redisTemplate.opsForValue().get(key);

        if (PROCESSED.equals(currentValue)) {
            return new Claim(ClaimStatus.ALREADY_PROCESSED, null);
        }

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, processingValue, PROCESSING_TTL);

        if (Boolean.TRUE.equals(acquired)) {
            return new Claim(ClaimStatus.CLAIMED, token);
        }

        currentValue = redisTemplate.opsForValue().get(key);

        if (PROCESSED.equals(currentValue)) {
            return new Claim(ClaimStatus.ALREADY_PROCESSED, null);
        }

        return new Claim(ClaimStatus.IN_PROGRESS, null);
    }

    public boolean markProcessed(String eventId, String token) {

        Long result = redisTemplate.execute(
                MARK_PROCESSED_SCRIPT,
                List.of(buildKey(eventId)),
                "PROCESSING:" + token,
                PROCESSED,
                String.valueOf(PROCESSED_TTL.toSeconds())
        );

        return Long.valueOf(1).equals(result);
    }

    public void release(String eventId, String token) {

        redisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(buildKey(eventId)),
                "PROCESSING:" + token
        );
    }

    private String buildKey(String eventId) {
        return "message-created:processed:" + eventId;
    }

    public enum ClaimStatus {
        CLAIMED,
        ALREADY_PROCESSED,
        IN_PROGRESS
    }

    public record Claim(
            ClaimStatus status,
            String token
    ) {}
}
