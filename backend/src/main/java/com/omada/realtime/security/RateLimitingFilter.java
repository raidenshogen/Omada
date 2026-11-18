package com.omada.realtime.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-IP rate limiter for auth endpoints.
 * Counts requests per (IP, endpoint) in Redis with a 1-minute window.
 * Exceeding the limit returns HTTP 429 Too Many Requests.
 *
 * Limits:
 *  - POST /api/auth/login    : 5 requests per minute per IP
 *  - POST /api/auth/register : 3 requests per minute per IP
 *  - POST /api/auth/refresh  : 10 requests per minute per IP
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final ConcurrentHashMap<String, Integer> LIMITS = new ConcurrentHashMap<>();
    static {
        LIMITS.put("POST:/api/auth/login", 5);
        LIMITS.put("POST:/api/auth/register", 3);
        LIMITS.put("POST:/api/auth/refresh", 10);
    }

    private final StringRedisTemplate redis;

    public RateLimitingFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return !LIMITS.containsKey(method + ":" + path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = getClientIp(req);
        String key = "rate_limit:" + ip + ":" + req.getMethod() + ":" + req.getRequestURI();
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        int limit = LIMITS.get(req.getMethod() + ":" + req.getRequestURI());
        if (count > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Too many requests. Try again later.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
