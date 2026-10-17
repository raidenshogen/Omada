package com.omada.realtime.service;

import com.omada.realtime.config.KafkaConfig;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.event.ActivityCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActivityProducer {
    private final KafkaTemplate<String, Activity> kafka;
    private final ApplicationEventPublisher events;

    public ActivityProducer(KafkaTemplate<String, Activity> kafka, ApplicationEventPublisher events){
        this.kafka = kafka;
        this.events = events;
    }

    /**
     * Publishes an activity to Kafka for durable materialization into the feed
     * collection, and fires a Spring event so it can also be pushed to any
     * clients currently watching the workspace's live feed.
     */
    public void publish(Activity activity){
        kafka.send(KafkaConfig.TOPIC_ACTIVITIES, activity.getBoardId(), activity);
        events.publishEvent(new ActivityCreatedEvent(activity));
    }
}
