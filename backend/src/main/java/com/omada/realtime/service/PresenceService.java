package com.omada.realtime.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Presence backed by Redis with per-user TTL keys
 * (presence:{scope}:{resourceId}:{userId} → display name, TTL ~45s), where
 * scope is "board" or "doc". Clients send a heartbeat every ~25s; a crashed
 * client's entry expires by itself. Snapshot = all live keys for the resource.
 */
@Service
public class PresenceService {

    private static final Duration TTL = Duration.ofSeconds(45);
    private static final String KEY_PREFIX = "presence:";

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Marks a user as present on a resource (also used as the heartbeat). */
    public void join(String scope, String resourceId, String userId, String name) {
        redis.opsForValue().set(key(scope, resourceId, userId),
                name == null || name.isBlank() ? "Anonymous" : name, TTL);
    }

    /** Removes a user from a resource's presence set. */
    public void leave(String scope, String resourceId, String userId) {
        redis.delete(key(scope, resourceId, userId));
    }

    /** All users currently present on a resource, as {userId, name} pairs. */
    public List<PresenceUser> list(String scope, String resourceId) {
        String prefix = prefix(scope, resourceId);
        Set<String> keys = redis.keys(prefix + "*");
        List<PresenceUser> users = new ArrayList<>();
        if (keys != null) {
            for (String k : keys) {
                String userId = k.substring(prefix.length());
                users.add(new PresenceUser(userId, redis.opsForValue().get(k)));
            }
        }
        return users;
    }

    private static String prefix(String scope, String resourceId) {
        return KEY_PREFIX + scope + ":" + resourceId + ":";
    }

    private static String key(String scope, String resourceId, String userId) {
        return prefix(scope, resourceId) + userId;
    }

    /** One present user: id + display name. */
    public record PresenceUser(String userId, String name) {
    }
}
