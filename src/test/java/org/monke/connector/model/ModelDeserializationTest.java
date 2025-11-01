package org.monke.connector.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Tests for deserialization of model classes.</p>
 * <p>Uses a sample JSON file.</p>
 * <p>Only a subset of properties is mapped into entities.</p>
 */
public class ModelDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    public void issueSerializationTest() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/issue.json");
        assertThat(inputStream).isNotNull();

        Issue issue = objectMapper.readValue(inputStream, Issue.class);

        assertThat(issue.getId()).isEqualTo(536763038);
        assertThat(issue.getUser().getClass()).isEqualTo(User.class);
        assertThat(issue.getAssignee().getClass()).isEqualTo(Assignee.class);
        assertThat(issue.getLabels().getFirst().getClass()).isEqualTo(Label.class);
    }
}
