package org.monke.connector.integration.common;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.policy.AllConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.connector.policy.ConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.ConnectorConfig;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.ConnectRestServer;
import org.apache.kafka.connect.runtime.rest.RestClient;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.ConnectorTaskId;
import org.apache.kafka.connect.util.FutureCallback;
import org.apache.kafka.connect.util.TopicAdmin;
import org.monke.connector.integration.config.TestWorkerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Connect runtime implementation.
 *
 * <p> Methods :
 * <ul>
 *     <li> {@link #start(Map)} : Initializes and starts the Connect runtime components including Worker, Herder, and REST server.
 *     <li> {@link #stop()} : Stops the Connect runtime components gracefully.
 *     <li> {@link #createConnector(Map)} : Creates a new connector with the given configuration.
 *     <li> {@link #restartTask(String, int)} : Restarts a specific task of a connector.
 * </ul>
 * 
 * <p> An {@link OffsetBackingStore} creates the connector offset management mechanism through the offset topic.
 */
@Slf4j
@NoArgsConstructor
public final class ConnectRunner {

    private boolean started = false;

    private Herder herder;                      // Manages connector lifecycle.
    private Worker worker;                      // Executes connector tasks.
    private ConnectRestServer restServer;       // REST API (optional for tests).
    private Connect<StandaloneHerder> connect;  // Main Connect runtime.

    /**
     * Creates a new connector with the given configuration.
     *
     * <p> Uses Herder to submit the connector configuration to runtime and waits for the operation to complete.
     *
     * <p> Connect runtime must be started before invoking this method.
     */
    public void createConnector(Map<String, String> config) throws ExecutionException, InterruptedException {
        final String connectorName = config.get(ConnectorConfig.NAME_CONFIG);

        if (!started) {
            throw new IllegalStateException("Connect runtime not started");
        }
        if (connectorName == null) {
            throw new IllegalArgumentException("Connector name is required");
        }

        // Defines a callback to handle connector creation result.
        FutureCallback<Herder.Created<ConnectorInfo>> creationCallback = new FutureCallback<>((error, info) -> {
            if (error != null) {
                log.error("Failed to create connector: {}", connectorName, error);

            } else {
                log.info("Created connector {}", info.result().name());
            }
        });

        // Submits connector creation to herder.
        herder.putConnectorConfig(connectorName, config, false, creationCallback);

        // Waits for completion.
        Herder.Created<ConnectorInfo> connectorInfoCreated = creationCallback.get();

        if (!connectorInfoCreated.created()) {
            throw new RuntimeException("Failed to create connector: " + connectorName);
        }
    }

    /**
     * Restarts a specific task of a connector.
     *
     * <p> Uses Herder to restart the specified task and waits for the operation to complete.
     *
     * <p> Connect runtime must be started before invoking this method.
     */
    public void restartTask(final String connector, final int task) {
        if (!started) {
            throw new IllegalStateException("Connect runtime not started");
        }

        log.info("Restarting task {}-{}", connector, task);

        final FutureCallback<Void> callback = new FutureCallback<>();

        herder.restartTask(new ConnectorTaskId(connector, task), callback);

        try {
            callback.get();
            log.info("Restarted task {}-{}", connector, task);

        } catch (Exception error) {
            log.error("Failed to restart task {}-{}", connector, task, error);
            throw new RuntimeException("Failed to restart task: " + connector + "-" + task, error);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void start(Map<String, String> workerProps) {
        log.info("Starting embedded Kafka Connect runtime with bootstrap servers: {}", workerProps.get(DistributedConfig.BOOTSTRAP_SERVERS_CONFIG));

        log.debug("Worker configuration: {}", workerProps);

        final Plugins plugins = new Plugins(workerProps);
        final DistributedConfig config = new DistributedConfig(workerProps);

        final String workerId = "test-worker";
        final String bootstrapServers = workerProps.get(DistributedConfig.BOOTSTRAP_SERVERS_CONFIG);

        log.debug("Initialized plugins & distributed config");

        final ConnectorClientConfigOverridePolicy overridePolicy = new AllConnectorClientConfigOverridePolicy();

        log.debug("Creating Kafka-based offset backing store for topic: {}", TestWorkerConfig.OFFSET_TOPIC);
        final OffsetBackingStore offsetBackingStore = createOffsetBackingStore(config, plugins, bootstrapServers);

        log.debug("Creating worker with ID: {}", workerId);
        worker = new Worker(workerId, Time.SYSTEM, plugins, config, offsetBackingStore, overridePolicy);

        log.debug("Creating herder for cluster: {}", config.kafkaClusterId()); // Fixed, provided by test container.
        herder = new StandaloneHerder(worker, config.kafkaClusterId(), overridePolicy);

        final RestClient restClient = new RestClient(config);
        restServer = new ConnectRestServer(10, restClient, workerProps);
        restServer.initializeServer();

        log.debug("Starting Connect runtime...");

        connect = new Connect(herder, restServer);
        connect.start();

        started = true;
        log.info("Embedded Kafka Connect runtime started successfully");
    }

    void stop() {
        log.info("Stopping embedded Kafka Connect runtime");

        connect.stop();

        started = false;
        log.info("Embedded Kafka Connect runtime stopped");
    }

    /**
     * Creates the connector offset management mechanism through the offset topic.
     */
    private OffsetBackingStore createOffsetBackingStore(final DistributedConfig config, final Plugins plugins, final String bootstrapServers) {

        final Supplier<TopicAdmin> topicAdminSupplier = () -> {
            final Map<String, Object> adminConfig = new HashMap<>();
            adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            adminConfig.put(AdminClientConfig.CLIENT_ID_CONFIG, "connect-worker-offset-admin");
            return new TopicAdmin(adminConfig);
        };

        final Supplier<String> clientIdSupplier = () -> TestWorkerConfig.OFFSET_TOPIC;

        final Converter keyConverter = plugins.newConverter(
            config,
            "key.converter",
            Plugins.ClassLoaderUsage.CURRENT_CLASSLOADER);

        final Map<String, Object> converterConfig = new HashMap<>();
        converterConfig.put("schemas.enable", "false");
        keyConverter.configure(converterConfig, true);

        final KafkaOffsetBackingStore offsetBackingStore = new KafkaOffsetBackingStore(
            topicAdminSupplier,
            clientIdSupplier,
            keyConverter);

        offsetBackingStore.configure(config);

        return offsetBackingStore;
    }
}
