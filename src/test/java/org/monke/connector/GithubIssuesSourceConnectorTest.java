package org.monke.connector;

import org.apache.kafka.common.config.Config;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.runtime.ConnectorConfig.*;
import static org.apache.kafka.connect.runtime.ConnectorConfig.CONNECTOR_CLASS_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.KEY_CONVERTER_CLASS_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.VALUE_CONVERTER_CLASS_CONFIG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.monke.connector.config.ConnectorConfig.BATCH_SIZE_CONFIG;
import static org.monke.connector.config.ConnectorConfig.OWNER_CONFIG;
import static org.monke.connector.config.ConnectorConfig.REPO_CONFIG;
import static org.monke.connector.config.ConnectorConfig.SINCE_TIMESTAMP_CONFIG;
import static org.monke.connector.config.ConnectorConfig.TOPIC_CONFIG;

public class GithubIssuesSourceConnectorTest {

    private static Map<String, String> createConnectorConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(NAME_CONFIG, "github-issues-source-connector");
        config.put(CONNECTOR_CLASS_CONFIG, "org.monke.connector.GithubIssuesSourceConnector");
        config.put(TASKS_MAX_CONFIG, "1");
        config.put(KEY_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter");
        config.put(VALUE_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter");
        config.put(TOPIC_CONFIG, "github-issues");
        config.put(OWNER_CONFIG, "kubernetes");
        config.put(REPO_CONFIG, "kubernetes");
        config.put(SINCE_TIMESTAMP_CONFIG, "2020-01-01T00:00:00Z");
        config.put(BATCH_SIZE_CONFIG, "1");
        return config;
    }

    @Test
    void should_validate_correct_config() {
        Map<String, String> config = createConnectorConfig();

        GithubIssuesSourceConnector connector = new GithubIssuesSourceConnector();

        Config validatedConfig = connector.validate(config);

        assertThat(
            validatedConfig.configValues().stream()
                .filter(c -> !c.errorMessages().isEmpty())
                .count()
        ).isEqualTo(0);
    }

    @Test
    void should_invalidate_bad_config() {
        Map<String, String> config = createConnectorConfig();

        config.put(TASKS_MAX_CONFIG, "invalid.number");

        GithubIssuesSourceConnector connector = new GithubIssuesSourceConnector();

        Config validatedConfig = connector.validate(config);

        assertThat(
            validatedConfig.configValues().stream()
                .filter(c -> !c.errorMessages().isEmpty())
                .count()
        ).isGreaterThan(0);
    }
}
