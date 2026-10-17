package com.omada.realtime.config;

import com.omada.realtime.dto.response.ActivityResponse;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.event.ActivityCreatedEvent;
import com.omada.realtime.mapper.ActivityMapper;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes newly produced activities to the WebSocket layer: clients subscribed
 * to /topic/activity.{workspaceId} receive the new item immediately, so the
 * activity panel updates live without polling.
 */
@Component
public class ActivitySocketPublisher {

    public static final String ACTIVITY_TOPIC_PREFIX = "/topic/activity.";

    private final SimpMessagingTemplate messaging;
    private final ActivityMapper mapper;

    public ActivitySocketPublisher(SimpMessagingTemplate messaging, ActivityMapper mapper) {
        this.messaging = messaging;
        this.mapper = mapper;
    }

    @EventListener
    public void onActivityCreated(ActivityCreatedEvent event) {
        Activity activity = event.activity();
        if (activity.getWorkspaceId() == null) return;
        ActivityResponse payload = mapper.toResponse(activity);
        messaging.convertAndSend(ACTIVITY_TOPIC_PREFIX + activity.getWorkspaceId(), payload);
    }
}
