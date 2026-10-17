package com.omada.realtime.config;

import com.omada.realtime.event.CardChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forwards card domain events to the WebSocket layer: every client
 * subscribed to /topic/board.{boardId} receives the change so open
 * boards stay in sync in realtime.
 */
@Component
public class BoardSocketPublisher {

    public static final String BOARD_TOPIC_PREFIX = "/topic/board.";

    private final SimpMessagingTemplate messaging;

    public BoardSocketPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Sends one card change to the board topic as a JSON payload. */
    @EventListener
    public void onCardChanged(CardChangedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", event.type());
        payload.put("boardId", event.boardId());
        payload.put("listId", event.listId());
        payload.put("previousListId", event.previousListId());
        payload.put("card", event.card());
        messaging.convertAndSend(BOARD_TOPIC_PREFIX + event.boardId(), payload);
    }
}
