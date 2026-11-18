package com.omada.realtime.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
/** JWT utility for access and refresh tokens.
 * Access token lifetime 15 minutes, refresh token lifetime 7 days.
 */
public class JwtUtil {
// Secret comes from the environment (JWT_SECRET); the fallback is for local
// dev only and must never reach a real deployment.
private static final String SECRET = System.getenv().getOrDefault(
        "JWT_SECRET", "change-me-to-32byte-secret-key-for-demo");
private static final long ACCESS_EXP_MS = 15 * 60 * 1000L;
private static final long REFRESH_EXP_MS = 7L * 24 * 60 * 60 * 1000;
private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

/** Generate access token for subject. */
public static String generateAccess(String subject) {
    return buildToken(subject, ACCESS_EXP_MS);
}

/** Generate refresh token for subject. */
public static String generateRefresh(String subject) {
    return buildToken(subject, REFRESH_EXP_MS);
}

private static String buildToken(String subject, long expMs) {
    return Jwts.builder()
            .setSubject(subject)
            .setId(UUID.randomUUID().toString())  // ADD: random JTI makes each token unique
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expMs))
            .signWith(KEY)
            .compact();
}

    /** Validate token and return subject or null. */
    public static String validate(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
