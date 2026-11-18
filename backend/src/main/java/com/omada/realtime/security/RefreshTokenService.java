package com.omada.realtime.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed refresh token store for rotation and revocation.
 * Each issued refresh token is stored with key {@code refresh_token:{token}}
 * and a 7-day TTL. On refresh, the old token is revoked (deleted) and a new
 * one is stored — so a refresh token can only be used once.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    public RefreshTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Stores a refresh token bound to a user, with a 7-day TTL. */
    public void store(String token, String userId) {
        redis.opsForValue().set(KEY_PREFIX + token, userId, TTL);
    }

    /** Returns true if the refresh token exists and has not been revoked. */
    public boolean validate(String token) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + token));
    }

    /** Revokes a refresh token (deletes it from Redis). No-op if unknown. */
    public void revoke(String token) {
        redis.delete(KEY_PREFIX + token);
    }
}
