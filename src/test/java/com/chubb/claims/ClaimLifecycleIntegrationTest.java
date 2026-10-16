package com.chubb.claims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: real controllers, services, JPA/H2 and an embedded Kafka broker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"claim-created", "claim-assigned", "claim-approved", "claim-rejected",
        "claim-settled"}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ClaimLifecycleIntegrationTest {

    private static final List<String> TOPICS =
            List.of("claim-created", "claim-assigned", "claim-approved", "claim-rejected", "claim-settled");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private static String claimJson(String amount, String exposure) {
        return """
                {"claimantName":"Jane Doe","claimantEmail":"jane@example.com","policyNumber":"POL-1",
                 "description":"Rear-end collision","incidentDate":"%s","claimAmount":%s,"liabilityExposure":%s}
                """.formatted(LocalDate.now().minusDays(1), amount, exposure);
    }

    private JsonNode createClaim(String exposure) throws Exception {
        String body = mockMvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimJson("2000.00", exposure)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private void transition(long id, String action, String body) throws Exception {
        var request = put("/api/claims/" + id + "/" + action);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        mockMvc.perform(request).andExpect(status().isOk());
    }

    private JsonNode dashboard() throws Exception {
        String body = mockMvc.perform(get("/api/dashboard/workload")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void createClaim_startsAsNew() throws Exception {
        JsonNode claim = createClaim("500.00");

        assertThat(claim.get("claimNumber").asText()).startsWith("CLM-");
        assertThat(claim.get("status").asText()).isEqualTo("NEW");
        mockMvc.perform(get("/api/claims/claim-number/" + claim.get("claimNumber").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTransition_returns409() throws Exception {
        JsonNode claim = createClaim("500.00");

        mockMvc.perform(put("/api/claims/" + claim.get("id").asLong() + "/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NEW -> APPROVED")));
    }

    @Test
    void dashboard_reflectsNewOpenClaim() throws Exception {
        JsonNode before = dashboard();

        createClaim("750.00");

        JsonNode after = dashboard();
        assertThat(after.get("totalOpenClaims").asLong()).isEqualTo(before.get("totalOpenClaims").asLong() + 1);
        assertThat(after.get("totalLiabilityExposure").decimalValue())
                .isEqualByComparingTo(before.get("totalLiabilityExposure").decimalValue().add(new java.math.BigDecimal("750.00")));
        assertThat(after.get("claimsByStatus").get("NEW").asLong())
                .isEqualTo(before.get("claimsByStatus").get("NEW").asLong() + 1);
    }

    @Test
    void fullLifecycle_publishesAnEventToEachTopic() throws Exception {
        Map<String, Object> props = new HashMap<>(KafkaTestUtils.consumerProps("it-group", "true", embeddedKafka));
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            embeddedKafka.consumeFromEmbeddedTopics(consumer, TOPICS.toArray(String[]::new));

            JsonNode settled = createClaim("1000.00");
            long settledId = settled.get("id").asLong();
            transition(settledId, "assign", "{\"officer\":\"John Smith\"}");
            transition(settledId, "under-review", null);
            transition(settledId, "approve", null);
            transition(settledId, "settle", null);

            JsonNode rejected = createClaim("300.00");
            long rejectedId = rejected.get("id").asLong();
            transition(rejectedId, "under-review", null);
            transition(rejectedId, "reject", "{\"note\":\"Policy lapsed\"}");

            Set<String> mine = Set.of(settled.get("claimNumber").asText(), rejected.get("claimNumber").asText());
            List<ConsumerRecord<String, String>> all = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 20_000;
            while (all.size() < 6 && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> {
                    if (mine.contains(r.key())) {
                        all.add(r);
                    }
                });
            }

            assertThat(all).extracting(ConsumerRecord::topic).containsExactlyInAnyOrder(
                    "claim-created", "claim-created", "claim-assigned", "claim-approved", "claim-settled",
                    "claim-rejected");
            String settledNumber = settled.get("claimNumber").asText();
            ConsumerRecord<String, String> settledEvent = all.stream()
                    .filter(r -> r.topic().equals("claim-settled")).findFirst().orElseThrow();
            assertThat(settledEvent.key()).isEqualTo(settledNumber);
            JsonNode payload = objectMapper.readTree(settledEvent.value());
            assertThat(payload.get("claimNumber").asText()).isEqualTo(settledNumber);
            assertThat(payload.get("assignedOfficer").asText()).isEqualTo("John Smith");
            assertThat(payload.get("occurredAt").asText()).contains("T");

            JsonNode rejectedPayload = objectMapper.readTree(all.stream()
                    .filter(r -> r.topic().equals("claim-rejected")).findFirst().orElseThrow().value());
            assertThat(rejectedPayload.get("reason").asText()).isEqualTo("Policy lapsed");
        }
    }

    @Test
    void moreInfoCycle_claimantSubmitsInformationAndOfficerWorkloadUpdates() throws Exception {
        String officer = "Priya Nair " + System.nanoTime();
        JsonNode claim = createClaim("800.00");
        long id = claim.get("id").asLong();

        transition(id, "assign", "{\"officer\":\"" + officer + "\"}");
        transition(id, "under-review", null);
        transition(id, "request-more-info", "{\"note\":\"Please send the repair invoice\"}");

        mockMvc.perform(get("/api/officers/{officer}/workload", officer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openClaims").value(1))
                .andExpect(jsonPath("$.totalLiabilityExposure").value(800.0))
                .andExpect(jsonPath("$.claimsByStatus.MORE_INFO_REQUIRED").value(1));

        mockMvc.perform(post("/api/claims/" + id + "/additional-info").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"information\":\"Invoice number INV-42 attached\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        mockMvc.perform(get("/api/claims/" + id + "/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].note")
                        .value("Additional information from claimant: Invoice number INV-42 attached"));

        mockMvc.perform(post("/api/claims/" + id + "/additional-info").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"information\":\"again\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/officers/{officer}/workload", officer))
                .andExpect(jsonPath("$.claimsByStatus.UNDER_REVIEW").value(1))
                .andExpect(jsonPath("$.claimsByStatus.MORE_INFO_REQUIRED").value(0));
    }
}
