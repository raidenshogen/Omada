package com.omada.realtime.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Reads the acting user from the current security context (the JWT subject is
 * the user id). Used by services to stamp authors, activity actors and events.
 */
public final class ActorContext {

    private ActorContext() {
    }

    /** Authenticated user id, or null when unauthenticated. */
    public static String id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /** Authenticated user id, falling back to "system" for background work. */
    public static String idOrSystem() {
        String id = id();
        return id != null ? id : "system";
    }

    /** Authenticated user id as a UUID; null when absent or not a UUID. */
    public static UUID uuid() {
        String id = id();
        if (id == null) return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
