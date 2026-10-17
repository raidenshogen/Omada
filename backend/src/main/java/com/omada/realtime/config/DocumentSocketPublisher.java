package com.omada.realtime.config;

import com.omada.realtime.event.DocumentChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forwards document/block domain events to the WebSocket layer: every client
 * subscribed to /topic/doc.{documentId} receives the delta so open editors
 * stay in sync ("simple OT" — versioned block deltas).
 */
@Component
public class DocumentSocketPublisher {

    public static final String DOCUMENT_TOPIC_PREFIX = "/topic/doc.";

    private final SimpMessagingTemplate messaging;

    public DocumentSocketPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @EventListener
    public void onDocumentChanged(DocumentChangedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", event.type());
        payload.put("documentId", event.documentId());
        payload.put("workspaceId", event.workspaceId());
        payload.put("block", event.block());
        messaging.convertAndSend(DOCUMENT_TOPIC_PREFIX + event.documentId(), payload);
    }
}
