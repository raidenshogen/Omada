package com.omada.realtime.config;

import com.omada.realtime.controller.PresenceController;
import com.omada.realtime.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * WebSocket lifecycle events: when a session disconnects abruptly (tab close,
 * network drop) we clear its presence entries (board and/or document) and
 * re-broadcast the affected presence lists. Normal navigation sends LEAVE.
 */
@Component
public class WebSocketEventHandler {

    private final PresenceService presenceService;
    private final PresenceController presenceController;

    public WebSocketEventHandler(PresenceService presenceService, PresenceController presenceController) {
        this.presenceService = presenceService;
        this.presenceController = presenceController;
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String user = event.getUser() != null ? event.getUser().getName() : null;
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (sessionId == null || user == null || attrs == null) {
            return;
        }
        Object boardId = attrs.get("presenceBoardId");
        if (boardId instanceof String board && !board.isBlank()) {
            presenceService.leave("board", board, user);
            presenceController.broadcast(board);
        }
        Object docId = attrs.get("presenceDocId");
        if (docId instanceof String doc && !doc.isBlank()) {
            presenceService.leave("doc", doc, user);
            presenceController.broadcastDoc(doc);
        }
    }
}
