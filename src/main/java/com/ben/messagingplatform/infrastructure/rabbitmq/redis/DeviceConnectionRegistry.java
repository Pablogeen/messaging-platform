
package com.ben.messagingplatform.infrastructure.rabbitmq.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeviceConnectionRegistry {

    private final StringRedisTemplate redisTemplate;

    private static final long SESSION_TTL_SECONDS = 1800;

    private static final String REGISTER_CONNECTION_SCRIPT = """
            local generation = redis.call('INCR', KEYS[1])

            redis.call('HSET', KEYS[2],
                'userId', ARGV[1],
                'deviceId', ARGV[2],
                'sessionId', ARGV[3],
                'serverId', ARGV[4],
                'generation', generation
            )

            redis.call('EXPIRE', KEYS[2], ARGV[5])
            redis.call('SADD', KEYS[3], ARGV[2])

            return generation
            """;

    private static final String REMOVE_CONNECTION_SCRIPT = """
            local currentGeneration =
                redis.call('HGET', KEYS[1], 'generation')

            if currentGeneration == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[2], ARGV[2])
                return 1
            end

            return 0
            """;

    private static final String HEARTBEAT_SCRIPT = """
            local currentGeneration =
                redis.call('HGET', KEYS[1], 'generation')

            if currentGeneration == ARGV[1] then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
                return 1
            end

            return 0
            """;

    public Long registerConnection(
            Long userId,
            String deviceId,
            String sessionId,
            String serverId
    ) {

        String generationKey =
                "user:" + userId + ":device:" + deviceId + ":generation";

        String ownershipKey =
                "user:" + userId + ":device:" + deviceId + ":ownership";

        String devicesKey = "user:" + userId + ":devices";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(REGISTER_CONNECTION_SCRIPT);
        script.setResultType(Long.class);

        return redisTemplate.execute(
                script,
                List.of(generationKey, ownershipKey, devicesKey),
                userId.toString(),
                deviceId,
                sessionId,
                serverId,
                String.valueOf(SESSION_TTL_SECONDS)
        );
    }

    public boolean removeConnection(
            Long userId,
            String deviceId,
            Long generation
    ) {

        String ownershipKey =
                "user:" + userId + ":device:" + deviceId + ":ownership";

        String devicesKey = "user:" + userId + ":devices";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(REMOVE_CONNECTION_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(ownershipKey, devicesKey),
                generation.toString(),
                deviceId
        );

        return Long.valueOf(1).equals(result);
    }

    public boolean refreshConnection(
            Long userId,
            String deviceId,
            Long generation
    ) {

        String ownershipKey =
                "user:" + userId + ":device:" + deviceId + ":ownership";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(HEARTBEAT_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(ownershipKey),
                generation.toString(),
                String.valueOf(SESSION_TTL_SECONDS)
        );

        return Long.valueOf(1).equals(result);
    }

    public DeviceConnection getConnection(
            Long userId,
            String deviceId
    ) {

        String ownershipKey =
                "user:" + userId + ":device:" + deviceId + ":ownership";

        List<Object> values = redisTemplate.opsForHash().multiGet(
                ownershipKey,
                List.of(
                        "userId",
                        "deviceId",
                        "sessionId",
                        "serverId",
                        "generation"
                )
        );

        if (values == null ||
                values.stream().anyMatch(value -> value == null)) {
            return null;
        }

        return new DeviceConnection(
                Long.valueOf(values.get(0).toString()),
                values.get(1).toString(),
                values.get(2).toString(),
                values.get(3).toString(),
                Long.valueOf(values.get(4).toString())
        );
    }

    public List<DeviceConnection> getActiveConnections(Long userId) {

        String devicesKey = "user:" + userId + ":devices";

        Set<String> deviceIds =
                redisTemplate.opsForSet().members(devicesKey);

        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }

        List<DeviceConnection> connections = new ArrayList<>();

        for (String deviceId : deviceIds) {

            DeviceConnection connection =
                    getConnection(userId, deviceId);

            if (connection == null) {
                redisTemplate.opsForSet().remove(
                        devicesKey,
                        deviceId
                );
                continue;
            }

            connections.add(connection);
        }

        return connections;
    }
}
