package com.omada.realtime.controller;

import com.omada.realtime.service.PresenceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * STOMP presence endpoint. Clients publish to /app/board/{boardId}/presence or
 * /app/doc/{docId}/presence with {action: JOIN|HEARTBEAT|LEAVE, name}; the
 * server keeps Redis presence fresh and broadcasts the full user list to
 * /topic/presence.{boardId} or /topic/docpresence.{docId}. The userId always
 * comes from the authenticated STOMP session (JWT at CONNECT), never the payload.
 */
@Controller
public class PresenceController {

    public static final String PRESENCE_TOPIC_PREFIX = "/topic/presence.";
    public static final String DOC_PRESENCE_TOPIC_PREFIX = "/topic/docpresence.";

    private static final String SCOPE_BOARD = "board";
    private static final String SCOPE_DOC = "doc";

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messaging;

    public PresenceController(PresenceService presenceService, SimpMessagingTemplate messaging) {
        this.presenceService = presenceService;
        this.messaging = messaging;
    }

    /** Incoming presence action; userId is taken from the session principal. */
    public record PresenceMessage(String action, String name) {
    }

    @MessageMapping("/board/{boardId}/presence")
    public void presence(@DestinationVariable String boardId,
                         @Payload PresenceMessage msg,
                         Principal user,
                         SimpMessageHeaderAccessor accessor) {
        if (apply(SCOPE_BOARD, boardId, msg, user, accessor, "presenceBoardId")) {
            broadcast(boardId);
        }
    }

    @MessageMapping("/doc/{docId}/presence")
    public void docPresence(@DestinationVariable String docId,
                            @Payload PresenceMessage msg,
                            Principal user,
                            SimpMessageHeaderAccessor accessor) {
        if (apply(SCOPE_DOC, docId, msg, user, accessor, "presenceDocId")) {
            broadcastDoc(docId);
        }
    }

    /** Applies a presence action and records the followed resource for disconnect cleanup. */
    private boolean apply(String scope, String resourceId, PresenceMessage msg, Principal user,
                          SimpMessageHeaderAccessor accessor, String sessionAttr) {
        if (user == null || user.getName() == null) {
            return false; // unauthenticated session — CONNECT rejects those anyway
        }
        String userId = user.getName();
        String action = msg != null && msg.action() != null ? msg.action() : "JOIN";
        switch (action) {
            case "LEAVE" -> presenceService.leave(scope, resourceId, userId);
            case "HEARTBEAT", "JOIN" -> presenceService.join(scope, resourceId, userId,
                    msg != null ? msg.name() : null);
            default -> { /* unknown action — ignore */ }
        }
        if (accessor.getSessionId() != null) {
            accessor.getSessionAttributes().put(sessionAttr, resourceId);
        }
        return true;
    }

    /** Sends the current presence snapshot for a board to its topic. */
    public void broadcast(String boardId) {
        List<PresenceService.PresenceUser> users = presenceService.list(SCOPE_BOARD, boardId);
        messaging.convertAndSend(PRESENCE_TOPIC_PREFIX + boardId, users);
    }

    /** Sends the current presence snapshot for a document to its topic. */
    public void broadcastDoc(String docId) {
        List<PresenceService.PresenceUser> users = presenceService.list(SCOPE_DOC, docId);
        messaging.convertAndSend(DOC_PRESENCE_TOPIC_PREFIX + docId, users);
    }
}
