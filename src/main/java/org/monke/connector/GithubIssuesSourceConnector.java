package org.monke.connector;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.ExactlyOnceSupport;
import org.apache.kafka.connect.source.SourceConnector;
import org.monke.connector.config.ConnectorConfig;
import org.monke.connector.util.Version;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

// Class is exposed to Connect framework.
public class GithubIssuesSourceConnector extends SourceConnector {

    private ConnectorConfig config;

    @Override
    public ExactlyOnceSupport exactlyOnceSupport(Map<String, String> inputConfig) {
        return ExactlyOnceSupport.SUPPORTED;
    }

    @Override
    public String version() {
        return Version.getVersion();
    }

    /**
     * Called when the connector is started.
     *
     * @param inputConfig The configuration for the connector.
     */
    @Override
    public void start(Map<String, String> inputConfig) {
        config = new ConnectorConfig(inputConfig);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return GithubIssuesSourceTask.class;
    }

    /**
     * Generates the configurations for the tasks based on the number of tasks.
     *
     * @param i Max number of tasks.
     */
    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        return IntStream.range(0, i)
            .mapToObj(it -> config.originalsStrings())
            .toList();
    }

    @Override
    public void stop() {}

    @Override
    public ConfigDef config() {
        return ConnectorConfig.config();
    }
}
