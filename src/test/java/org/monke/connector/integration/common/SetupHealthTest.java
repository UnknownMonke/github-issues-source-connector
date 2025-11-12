package org.monke.connector.integration.common;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SetupHealthTest extends AbstractIT {

    @Test
    void should_init_cluster_correctly() throws ExecutionException, InterruptedException {

        // Verifies container is running.
        assert kafkaContainer.isRunning();

        // Verifies producer is up.
        producer.send(new ProducerRecord<>("test-topic", "key", "value")).get();

        // Verifies consumer is up.
        consumer.subscribe(Collections.singletonList("test-topic"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assert records.count() > 0;

        // Verifies admin client is up.
        Set<String> topics = adminClient.listTopics().names().get();
        assert topics.contains("test-topic");
    }
}
