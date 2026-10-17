package com.omada.realtime.event;

import com.omada.realtime.entity.Activity;

/**
 * Domain event fired when a new Activity is produced (card/comment changes).
 * Carries the Activity so the WebSocket layer can broadcast it to clients
 * subscribed to the workspace's live activity feed.
 */
public record ActivityCreatedEvent(Activity activity) {
}
