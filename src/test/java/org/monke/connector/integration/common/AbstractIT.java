package org.monke.connector.integration.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.monke.connector.integration.config.TestClusterConfig;
import org.monke.connector.integration.config.TestConsumerProperties;
import org.monke.connector.integration.config.TestProducerProperties;
import org.monke.connector.integration.config.TestWorkerConfig;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Abstract integration base class for Kafka Connect connector tests.
 * 
 * <p> Wraps each tests with setup and teardown for :
 * <ul>
 *     <li> Test cluster running in a {@link Testcontainers} with supplied configuration through {@link TestClusterConfig}.
 *     <li> Producer, consumer, and admin client for test interactions with supplied configuration
 *          through {@link TestProducerProperties}, {@link TestConsumerProperties}.
 *     <li> Connect runtime worker with supplied configuration through {@link TestWorkerConfig}
 * </ul>
 *
 * <p> Provides utility methods for creating topics and connector configurations.
 *
 * <p> Fully generic and usable for any custom Connect plugins (connectors, SMTs...).
 */
@Slf4j
@Testcontainers
public abstract class AbstractIT {

    protected KafkaProducer<String, String> producer;
    protected KafkaConsumer<String, String> consumer;
    protected AdminClient adminClient;
    protected ConnectRunner connectRunner;

    @Container
    protected final KafkaContainer kafkaContainer = new KafkaContainer(TestClusterConfig.KAFKA_IMAGE_NAME)
        .withNetwork(Network.newNetwork())                  // Each test suite runs in its own Docker network.
        .withStartupTimeout(Duration.ofSeconds(10))         // Ensures Kafka is ready before tests run.
        .withExposedPorts(TestClusterConfig.CLUSTER_PORT);


    @BeforeEach
    void setup() throws Exception {
        setupClients();
        setupTopics();
        setupKafkaConnect();
    }

    @AfterEach
    void teardown() {
        if (connectRunner != null) connectRunner.stop();
        if (producer != null) producer.close();
        if (consumer != null) consumer.close();
    }

    /**
     * Sets up Kafka producer, consumer, and admin client for test interactions.
     */
    private void setupClients() {
        producer = new KafkaProducer<>(TestProducerProperties.getProperties(kafkaContainer.getBootstrapServers()));
        consumer = new KafkaConsumer<>(TestConsumerProperties.getProperties(kafkaContainer.getBootstrapServers()));

        final Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        adminClient = AdminClient.create(adminProps);
    }

    /**
     * Sets up necessary Kafka topics for testing, including internal Kafka Connect topics.
     */
    private void setupTopics() throws ExecutionException, InterruptedException {
        List<String> topicNames = List.of(
            TestWorkerConfig.OFFSET_TOPIC,
            TestWorkerConfig.CONFIG_TOPIC,
            TestWorkerConfig.STATUS_TOPIC
        );

        final Set<String> existingTopics = adminClient.listTopics().names().get();

        List<NewTopic> topicsToCreate = topicNames.stream()
            .filter(name -> !existingTopics.contains(name))
            .map(name -> {
                NewTopic topic = new NewTopic(name, 1, (short) 1);

                if (isConnectInternalTopic(name)) {
                    Map<String, String> configs = new HashMap<>();
                    configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
                    topic.configs(configs);
                }
                return topic;
            })
            .toList();

        adminClient.createTopics(topicsToCreate).all().get();
    }

    /**
     * Sets up and starts the Kafka Connect runtime with the necessary worker configuration and plugins JARs paths.
     */
    private void setupKafkaConnect() {
        connectRunner = new ConnectRunner();

        connectRunner.start(
            TestWorkerConfig.getConfig(kafkaContainer.getBootstrapServers())
        );
    }

    /**
     * Topic creation utility for tests.
     */
    protected void createTopic(String topicName, int partitions) throws ExecutionException, InterruptedException {
        NewTopic topic = new NewTopic(topicName, partitions, (short) 1);
        adminClient.createTopics(List.of(topic)).all().get();
    }

    private boolean isConnectInternalTopic(String topicName) {
        return topicName.equals(TestWorkerConfig.OFFSET_TOPIC) ||
            topicName.equals(TestWorkerConfig.CONFIG_TOPIC) ||
            topicName.equals(TestWorkerConfig.STATUS_TOPIC);
    }
}
