package org.monke.connector.integration;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.monke.connector.integration.common.AbstractIT;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.connect.runtime.ConnectorConfig.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.monke.connector.config.ConnectorConfig.BATCH_SIZE_CONFIG;
import static org.monke.connector.config.ConnectorConfig.OWNER_CONFIG;
import static org.monke.connector.config.ConnectorConfig.REPO_CONFIG;
import static org.monke.connector.config.ConnectorConfig.SINCE_TIMESTAMP_CONFIG;
import static org.monke.connector.config.ConnectorConfig.TOPIC_CONFIG;

@Slf4j
public class GithubIssuesSourceConnectorIT extends AbstractIT {

    private static final String SOURCE_TOPIC = "github-issues";

    private static Map<String, String> createConnectorConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(NAME_CONFIG, "github-issues-source-connector");
        config.put(CONNECTOR_CLASS_CONFIG, "org.monke.connector.GithubIssuesSourceConnector");
        config.put(TASKS_MAX_CONFIG, "1");
        config.put(KEY_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter");
        config.put(VALUE_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter");
        config.put(TOPIC_CONFIG, SOURCE_TOPIC);
        config.put(OWNER_CONFIG, "kubernetes");
        config.put(REPO_CONFIG, "kubernetes");
        config.put(SINCE_TIMESTAMP_CONFIG, "2020-01-01T00:00:00Z");
        config.put(BATCH_SIZE_CONFIG, "1");
        return config;
    }

    @Test
    void should_publish_messages() throws ExecutionException, InterruptedException {
        createTopic(SOURCE_TOPIC, 1);

        consumer.subscribe(Collections.singletonList(SOURCE_TOPIC));

        // Initializes connector and starts tasks.
        connectRunner.createConnector(createConnectorConfig());

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

                assertThat(records.count()).isGreaterThan(0);
            });
    }

    @Test
    void should_publish_correct_records() throws ExecutionException, InterruptedException {
        createTopic(SOURCE_TOPIC, 1);

        consumer.subscribe(Collections.singletonList(SOURCE_TOPIC));

        // Initializes connector and starts tasks.
        connectRunner.createConnector(createConnectorConfig());

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

                for (var record : records) {
                    log.info("Received record: key = {}, value = {}", record.key(), record.value());

                    assertThat(record.key()).isNotNull();
                    assertThat(record.value()).isNotNull();

                    assertThat(record.key()).contains("\"owner\":\"kubernetes\"");
                    assertThat(record.key()).contains("\"repository\":\"kubernetes\"");

                    // Basic checks for expected fields in the JSON value.
                    assertThat(record.value()).contains("\"id\":");
                    assertThat(record.value()).contains("\"number\":");
                    assertThat(record.value()).contains("\"title\":");
                    assertThat(record.value()).contains("\"state\":");
                    assertThat(record.value()).contains("\"created_at\":");
                    assertThat(record.value()).contains("\"updated_at\":");
                }
            });
    }
}
