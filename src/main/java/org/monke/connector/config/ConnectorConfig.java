package org.monke.connector.config;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.monke.connector.config.validator.BatchSizeValidator;
import org.monke.connector.config.validator.TimestampValidator;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Connector configuration definition :
 * <ul>
 *     <li> Accesses needed common and specific properties.
 *     <li> Defines allowed properties to be validated when receiving standalone or distributed configuration.
 * </ul>
 */
public class ConnectorConfig extends AbstractConfig {

    public static final String NAME_CONFIG = "name";
    public static final String TASKS_MAX_CONFIG = "tasks.max";
    public static final String CONNECTOR_CLASS_CONFIG = "connector.class";

    public static final String TOPIC_CONFIG = "topic";
    public static final String OWNER_CONFIG = "github.owner";
    public static final String REPO_CONFIG = "github.repo";
    public static final String AUTH_USERNAME_CONFIG = "auth.username";
    public static final String AUTH_PASSWORD_CONFIG = "auth.password";
    public static final String SINCE_TIMESTAMP_CONFIG = "since.timestamp";
    public static final String BATCH_SIZE_CONFIG = "batch.size";

    private static final String NAME_DOC = "Name of the connector.";
    private static final String TASKS_MAX_DOC = "Maximum number of tasks to launch for this connector.";
    private static final String CONNECTOR_CLASS_DOC = "Connector FQCN.";

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

    /**
     * Defines allowed configuration options for the connector.
     */
    public static ConfigDef config() {
        return new ConfigDef()
            .define(NAME_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, NAME_DOC)
            .define(TASKS_MAX_CONFIG, ConfigDef.Type.INT, 1, ConfigDef.Importance.HIGH, TASKS_MAX_DOC)
            .define(CONNECTOR_CLASS_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, CONNECTOR_CLASS_DOC)
            .define(TOPIC_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, TOPIC_DOC)
            .define(OWNER_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, OWNER_DOC)
            .define(REPO_CONFIG, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, REPO_DOC)
            .define(AUTH_USERNAME_CONFIG, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, AUTH_USERNAME_DOC)
            .define(AUTH_PASSWORD_CONFIG, ConfigDef.Type.STRING, "", ConfigDef.Importance.HIGH, AUTH_PASSWORD_DOC)
            .define(SINCE_TIMESTAMP_CONFIG, ConfigDef.Type.STRING,
                ZonedDateTime.now().minusYears(1).toInstant().toString(),
                new TimestampValidator(),
                ConfigDef.Importance.HIGH, SINCE_TIMESTAMP_DOC)
            .define(BATCH_SIZE_CONFIG, ConfigDef.Type.INT,
                100,
                new BatchSizeValidator(),
                ConfigDef.Importance.LOW, BATCH_SIZE_DOC);
    }

    public String getTopic() {
        return this.getString(TOPIC_CONFIG);
    }
    public String getOwner() {
        return this.getString(OWNER_CONFIG);
    }
    public String getRepo() {
        return this.getString(REPO_CONFIG);
    }
    public String getAuthUsername() {
        return this.getString(AUTH_USERNAME_CONFIG);
    }
    public String getAuthPassword() {
        return this.getString(AUTH_PASSWORD_CONFIG);
    }
    public Instant getSince() {
        return Instant.parse(this.getString(SINCE_TIMESTAMP_CONFIG));
    }
    public int getBatchSize() {
        return this.getInt(BATCH_SIZE_CONFIG);
    }
}
