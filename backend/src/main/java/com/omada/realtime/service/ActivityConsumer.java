package com.omada.realtime.service;

import com.omada.realtime.config.KafkaConfig;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.repository.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes Activity events from Kafka and materializes them into the MongoDB
 * activity feed, which the ActivityService / REST feed API read from.
 * Idempotent: events whose id is already stored are skipped.
 */
@Service
public class ActivityConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityConsumer.class);

    private final ActivityRepository activityRepository;

    public ActivityConsumer(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * Handles one Activity event: saves it into the feed collection unless
     * an activity with the same id already exists (duplicate delivery).
     *
     * @param activity deserialized Activity event; null or id-less events are dropped
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ACTIVITIES,
            groupId = KafkaConfig.CONSUMER_GROUP,
            containerFactory = "activityListenerContainerFactory")
    public void onActivity(Activity activity) {
        if (activity == null || activity.getId() == null) {
            log.warn("Dropping malformed activity event: {}", activity);
            return;
        }
        if (activityRepository.existsById(activity.getId())) {
            return; // duplicate delivery — feed is already materialized
        }
        activityRepository.save(activity);
        log.debug("Materialized activity {} ({}) for board {}",
                activity.getId(), activity.getType(), activity.getBoardId());
    }
}
