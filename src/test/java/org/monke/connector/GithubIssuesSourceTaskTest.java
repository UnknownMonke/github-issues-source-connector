package org.monke.connector;

import org.apache.kafka.connect.source.SourceRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.monke.connector.config.ConnectorConfig;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GithubIssuesSourceTaskTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ConnectorConfig connectorConfig;

    @InjectMocks
    private GithubIssuesSourceTask githubIssuesSourceTask;


    @BeforeEach
    public void setup() {
        when(connectorConfig.getRepo()).thenReturn("kubernetes");
        when(connectorConfig.getOwner()).thenReturn("kubernetes");
        when(connectorConfig.getTopic()).thenReturn("github-issues");
    }

    @Test
    public void poll_should_successfully_parse_issues() throws InterruptedException {
        InputStream inputStream = getClass().getResourceAsStream("/issue.json");
        assertThat(inputStream).isNotNull();

        JSONObject issue = new JSONObject(new JSONTokener(inputStream));
        JSONArray issues = new JSONArray();
        issues.put(issue);

        when(httpClient.fetchIssues(anyInt(), any())).thenReturn(issues);

        githubIssuesSourceTask.nextQuerySince = Instant.parse("2020-01-01T01:03:41Z");

        List<SourceRecord> result = githubIssuesSourceTask.poll();

        assertThat(result).isNotNull();
    }
}
