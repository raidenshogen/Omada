package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.config.KafkaConfig;
import com.omada.realtime.entity.Activity;
import com.omada.realtime.repository.ActivityRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end Kafka flow test: publish an Activity event through the real producer,
 * wait for the consumer to materialize it into MongoDB, verify the feed contents.
 */
public class KafkaActivityFlowIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Activity> activityKafkaTemplate;

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void publishedActivity_isConsumed_andMaterializedInMongo() {
        Activity event = Activity.builder()
                .id(UUID.randomUUID())
                .workspaceId("ws-kafka-test")
                .boardId("board-kafka-test")
                .actorId("tester")
                .type("CARD_CREATED")
                .entityId("card-1")
                .createdAt(Instant.now())
                .build();

        activityKafkaTemplate.send(KafkaConfig.TOPIC_ACTIVITIES, event.getBoardId(), event);

        // Consumer is async — poll the Mongo feed until the event shows up
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() ->
                        assertThat(activityRepository.findByBoardIdOrderByCreatedAtDesc("board-kafka-test"))
                                .anyMatch(a -> a.getId().equals(event.getId()) && "CARD_CREATED".equals(a.getType())));
    }

    @Test
    void consumer_skipsDuplicateEvents_idempotent() {
        Activity event = Activity.builder()
                .id(UUID.randomUUID())
                .workspaceId("ws-dup-test")
                .boardId("board-dup-test")
                .actorId("tester")
                .type("CARD_UPDATED")
                .entityId("card-2")
                .createdAt(Instant.now())
                .build();

        // same event id sent twice — feed must contain exactly one copy
        activityKafkaTemplate.send(KafkaConfig.TOPIC_ACTIVITIES, event.getBoardId(), event);
        activityKafkaTemplate.send(KafkaConfig.TOPIC_ACTIVITIES, event.getBoardId(), event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() ->
                        assertThat(activityRepository.findByBoardIdOrderByCreatedAtDesc("board-dup-test"))
                                .anyMatch(a -> a.getId().equals(event.getId())));

        long copies = activityRepository.findByBoardIdOrderByCreatedAtDesc("board-dup-test").stream()
                .filter(a -> a.getId().equals(event.getId()))
                .count();
        assertThat(copies).isEqualTo(1);
    }
}
