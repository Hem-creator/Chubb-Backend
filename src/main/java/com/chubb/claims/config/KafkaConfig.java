package com.chubb.claims.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    /**
     * Values are serialised with the application's ObjectMapper so dates are ISO-8601, and without
     * type headers so non-Java consumers can read the messages.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null),
                new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic claimCreatedTopic(KafkaTopicsProperties topics) {
        return topic(topics.created());
    }

    @Bean
    public NewTopic claimAssignedTopic(KafkaTopicsProperties topics) {
        return topic(topics.assigned());
    }

    @Bean
    public NewTopic claimApprovedTopic(KafkaTopicsProperties topics) {
        return topic(topics.approved());
    }

    @Bean
    public NewTopic claimRejectedTopic(KafkaTopicsProperties topics) {
        return topic(topics.rejected());
    }

    @Bean
    public NewTopic claimSettledTopic(KafkaTopicsProperties topics) {
        return topic(topics.settled());
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
