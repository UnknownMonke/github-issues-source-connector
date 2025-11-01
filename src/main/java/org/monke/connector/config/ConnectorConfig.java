package org.monke.connector.config;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.monke.connector.config.validator.BatchSizeValidator;
import org.monke.connector.config.validator.TimestampValidator;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

public class ConnectorConfig extends AbstractConfig {

    public static final String TOPIC = "topic";
    public static final String OWNER = "github.owner";
    public static final String REPO = "github.repo";
    public static final String AUTH_USERNAME = "auth.username";
    public static final String AUTH_PASSWORD = "auth.password";
    public static final String SINCE_TIMESTAMP = "since.timestamp";
    public static final String BATCH_SIZE = "batch.size";

    private static final String TOPIC_DOC = "Kafka topic to publish issues to.";
    private static final String REPO_DOC = "GitHub repository to monitor (e.g., owner/repo).";
    private static final String OWNER_DOC = "Owner of the GitHub repository.";
    private static final String AUTH_USERNAME_DOC = "Optional GitHub username for authentication.";
    private static final String AUTH_PASSWORD_DOC = "Optional GitHub password or personal access token for authentication.";
    private static final String SINCE_TIMESTAMP_DOC =
        "Only issues updated at or after this time are returned. ISO 8601 format. Defaults to a year from first launch.";
    private static final String BATCH_SIZE_DOC = "Number of issues to fetch in each API call. Defaults to 100 (max value).";


    /**
     * Creates a new instance by resolving input configuration against the connector's configuration definition.
     *
     * @param inputConfig The connector input configuration.
     */
    public ConnectorConfig(Map<String, String> inputConfig) {
        super(config(), inputConfig);
    }

    public static ConfigDef config() {
        return new ConfigDef()
            .define(TOPIC, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, TOPIC_DOC)
            .define(OWNER, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, OWNER_DOC)
            .define(REPO, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, REPO_DOC)
            .define(AUTH_USERNAME, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, AUTH_USERNAME_DOC)
            .define(AUTH_PASSWORD, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, AUTH_PASSWORD_DOC)
            .define(SINCE_TIMESTAMP, ConfigDef.Type.STRING,
                ZonedDateTime.now().minusYears(1).toInstant().toString(),
                new TimestampValidator(),
                ConfigDef.Importance.HIGH, SINCE_TIMESTAMP_DOC)
            .define(BATCH_SIZE, ConfigDef.Type.INT,
                100,
                new BatchSizeValidator(),
                ConfigDef.Importance.LOW, BATCH_SIZE_DOC);
    }

    public String getTopic() {
        return this.getString(TOPIC);
    }

    public String getOwner() {
        return this.getString(OWNER);
    }

    public String getRepo() {
        return this.getString(REPO);
    }

    public String getAuthUsername() {
        return this.getString(AUTH_USERNAME);
    }

    public String getAuthPassword() {
        return this.getString(AUTH_PASSWORD);
    }

    public Instant getSince() {
        return Instant.parse(this.getString(SINCE_TIMESTAMP));
    }

    public int getBatchSize() {
        return this.getInt(BATCH_SIZE);
    }
}
