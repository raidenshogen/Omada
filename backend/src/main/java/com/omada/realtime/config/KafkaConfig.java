package com.omada.realtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omada.realtime.entity.Activity;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka wiring for the activity event bus.
 *
 * Producer: {@code ActivityProducer} publishes Activity events to {@link #TOPIC_ACTIVITIES}.
 * Consumer: {@code ActivityConsumer} materializes events into the Mongo activity feed.
 * Uses an explicitly built ObjectMapper (with JavaTimeModule) so Instant fields round-trip
 * correctly regardless of the web ObjectMapper configuration.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String TOPIC_ACTIVITIES = "activities";
    public static final String CONSUMER_GROUP = "omada-activity-feed";

    /** ObjectMapper for Kafka JSON (de)serialization; JavaTimeModule handles Instant fields. */
    private static ObjectMapper activityMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /** Producer factory: String keys (boardId) + JSON Activity values, acks=all for durability. */
    @Bean
    public ProducerFactory<String, Activity> activityProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(activityMapper()));
    }

    @Bean
    public KafkaTemplate<String, Activity> activityKafkaTemplate(ProducerFactory<String, Activity> activityProducerFactory) {
        return new KafkaTemplate<>(activityProducerFactory);
    }

    /** Consumer factory: group omada-activity-feed, earliest offset, JSON Activity values. */
    @Bean
    public ConsumerFactory<String, Activity> activityConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<Activity> valueDeserializer = new JsonDeserializer<>(Activity.class, activityMapper());
        valueDeserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Activity> activityListenerContainerFactory(
            ConsumerFactory<String, Activity> activityConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Activity> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(activityConsumerFactory);
        return factory;
    }

    /** Creates the activities topic at startup (3 partitions, keyed by boardId). */
    @Bean
    public NewTopics activitiesTopic() {
        return new NewTopics(TopicBuilder.name(TOPIC_ACTIVITIES).partitions(3).build());
    }
}
