package org.monke.connector.integration.config;

import org.apache.kafka.connect.runtime.distributed.DistributedConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic configuration for integration test worker.
 * Change if necessary.
 */
public final class TestWorkerConfig {

    public static final String CONFIG_TOPIC = "_connect-configs";
    public static final String OFFSET_TOPIC = "_connect-offsets";
    public static final String STATUS_TOPIC = "_connect-status";
    public static final String GROUP_ID = "test-worker-group";
    public static final String OFFSET_COMMIT_INTERVAL_MS = "5000";
    public static final String TASK_SHUTDOWN_GRACEFUL_TIMEOUT_MS = "5000";
    public static final String KEY_CONVERTER = "org.apache.kafka.connect.json.JsonConverter";
    public static final String VALUE_CONVERTER = "org.apache.kafka.connect.json.JsonConverter";
    public static final String PLUGIN_PATH = "./build/libs/";


    public static Map<String, String> getConfig(String bootstrapServers) {

        final Map<String, String> config = new HashMap<>();

        config.put(DistributedConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(DistributedConfig.OFFSET_COMMIT_INTERVAL_MS_CONFIG, OFFSET_COMMIT_INTERVAL_MS);
        config.put(DistributedConfig.TASK_SHUTDOWN_GRACEFUL_TIMEOUT_MS_CONFIG, TASK_SHUTDOWN_GRACEFUL_TIMEOUT_MS);

        // Default converters - connectors can override these.
        config.put(DistributedConfig.KEY_CONVERTER_CLASS_CONFIG, KEY_CONVERTER);
        config.put(DistributedConfig.VALUE_CONVERTER_CLASS_CONFIG, VALUE_CONVERTER);

        // Plugin path.
        config.put(DistributedConfig.PLUGIN_PATH_CONFIG, PLUGIN_PATH);

        config.put(DistributedConfig.GROUP_ID_CONFIG, GROUP_ID);

        config.put(DistributedConfig.CONFIG_TOPIC_CONFIG, CONFIG_TOPIC);
        config.put(DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG, OFFSET_TOPIC);
        config.put(DistributedConfig.STATUS_STORAGE_TOPIC_CONFIG, STATUS_TOPIC);

        return config;
    }
}
