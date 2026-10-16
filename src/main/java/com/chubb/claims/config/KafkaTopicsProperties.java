package com.chubb.claims.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Topic names, bound from {@code app.kafka.topics.*}. */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        @DefaultValue("claim-created") String created,
        @DefaultValue("claim-assigned") String assigned,
        @DefaultValue("claim-approved") String approved,
        @DefaultValue("claim-rejected") String rejected,
        @DefaultValue("claim-settled") String settled
) {
}
