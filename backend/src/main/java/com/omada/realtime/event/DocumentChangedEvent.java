package com.omada.realtime.event;

/**
 * Domain event for document/block changes. Broadcast to /topic/doc.{documentId}
 * so open editors apply the delta, mirroring CardChangedEvent for boards.
 */
public record DocumentChangedEvent(
        String type,
        String documentId,
        String workspaceId,
        Object block) {
}
