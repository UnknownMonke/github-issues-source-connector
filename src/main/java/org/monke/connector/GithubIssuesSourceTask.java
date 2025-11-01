package org.monke.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.json.JSONArray;
import org.monke.connector.config.ConnectorConfig;
import org.monke.connector.model.Issue;
import org.monke.connector.model.PullRequest;
import org.monke.connector.model.User;
import org.monke.connector.util.DateUtils;
import org.monke.connector.util.VersionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>Kafka Connect Source Task that polls GitHub Issues API for new or updated issues in a specified repository.</p>
 * <p>Maps issues to Kafka records with defined key and value schemas (see {@link Schemas}).</p>
 * <p>Maintains state using source partitions and offsets to ensure exactly-once delivery.</p>
 */
@Slf4j
public class GithubIssuesSourceTask extends SourceTask {

    protected Instant nextQuerySince;
    protected Integer lastIssueNumber;
    protected Integer nextPageToVisit = 1;
    protected Instant lastUpdatedAt;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ConnectorConfig config;
    private HttpClient client;


    @Override
    public String version() {
        return VersionUtils.getVersion();
    }

    @Override
    public void start(Map<String, String> map) {
        config = new ConnectorConfig(map);
        client = new HttpClient(config, new OkHttpClient());

        log.info("Initialized HttpClient...");
        resume();
    }

    private void resume() {
        // Gets the offset for the specific partition (repository).
        Map<String, Object> offset = context.offsetStorageReader().offset(sourcePartition());

        if (offset != null) {
            lastUpdatedAt = Instant.parse((String) offset.get(Schemas.UPDATED_AT));
            lastIssueNumber = Integer.parseInt((String) offset.get(Schemas.NUMBER));
            nextPageToVisit = Integer.parseInt((String) offset.get(Schemas.NEXT_PAGE));
            nextQuerySince = lastUpdatedAt;

            log.info("Resuming from offset. lastUpdatedAt: {}, lastIssueNumber: {}, nextPageToVisit: {}",
                lastUpdatedAt, lastIssueNumber, nextPageToVisit);

        } else {
            nextQuerySince = config.getSince();
            lastUpdatedAt = nextQuerySince;
            lastIssueNumber = -1;
            nextPageToVisit = 1;
        }
    }

    /**
     * <p>Main method. Polls for new issues and maps to a Kafka record with additional information, such as source specific partitions and offsets.</p>
     * <p>Only a subset of the entity is pushed to Kafka, as defined by the associated schemas (see {@link Schemas}).</p>
     */
    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        client.sleepIfNeeded();

        final List<SourceRecord> records = new ArrayList<>();

        JSONArray issues = client.fetchIssues(nextPageToVisit, nextQuerySince);

        log.info("Fetched {} record(s).", issues.length());

        for (Object obj : issues) {
            Issue issue = objectMapper.convertValue(obj, Issue.class);
            records.add(generateRecord(issue));
            lastUpdatedAt = issue.getUpdatedAt();
        }

        if (issues.length() == 100) { // Full batch, increments page.
            nextPageToVisit++;

        } else {
            nextQuerySince = lastUpdatedAt.plusSeconds(1);
            nextPageToVisit = 1;
            client.sleep();
        }
        return records;
    }

    @Override
    public void stop() {}

    private SourceRecord generateRecord(Issue issue) {
        return new SourceRecord(
            sourcePartition(),                  // Repository as source partition.
            sourceOffset(issue.getUpdatedAt()), // updated_at + next page as source offset.
            config.getTopic(),                  // Sink topic.
            null,                               // Sink topic partition. Inferred by framework.
            Schemas.KEY_SCHEMA,                 // Record key schema.
            mapRecordKey(issue),                // Record key.
            Schemas.VALUE_SCHEMA,               // Record value schema.
            mapRecordValue(issue),              // Record value.
            issue.getUpdatedAt().toEpochMilli() // Record timestamp.
        );
    }

    /**
     * Returns a map that represents the source partition.
     * Used to identify the partition for the task.
     * In this case, owner + repo.
     */
    private Map<String, String> sourcePartition() {
        return Map.of(
            Schemas.OWNER, config.getOwner(),
            Schemas.REPOSITORY, config.getRepo()
        );
    }

    /**
     * Returns a map that represents the source offset.
     * Used to track the progress of the task.
     * In this case, updated_at timestamp + next page to visit.
     */
    private Map<String, String> sourceOffset(Instant updatedAt) {
        return Map.of(
            Schemas.UPDATED_AT, DateUtils.getMostRecent(updatedAt, nextQuerySince).toString(),
            Schemas.NUMBER, lastIssueNumber.toString(),
            Schemas.NEXT_PAGE, nextPageToVisit.toString()
        );
    }

    /**
     * <p>Builds Kafka record key from source entity.</p>
     * <p>An issue is uniquely identified by its owner + repo and issue number.</p>
     * <p>Key is validated using the defined key schema.</p>
     */
    private Struct mapRecordKey(Issue issue) {

        return new Struct(Schemas.KEY_SCHEMA)
            .put(Schemas.OWNER, config.getOwner())
            .put(Schemas.REPOSITORY, config.getRepo())
            .put(Schemas.NUMBER, issue.getNumber());
    }

    /**
     * <p>Builds Kafka record value from source entity.</p>
     * <p>Only a subset of the issue entity is pushed to Kafka, as defined by the associated value schema (see {@link Schemas}).</p>
     * <p>Value is validated using the defined value schema.</p>
     * <p>Nested structs are used for user and pull request information.</p>
     * <p>Pull request is optional, as not all issues are pull requests.
     * User is mandatory, as all issues have a user.</p>
     */
    public Struct mapRecordValue(Issue issue) {

        Struct valueStruct = new Struct(Schemas.VALUE_SCHEMA)
            .put(Schemas.URL, issue.getUrl())
            .put(Schemas.TITLE, issue.getTitle())
            .put(Schemas.CREATED_AT, Date.from(issue.getCreatedAt()))
            .put(Schemas.UPDATED_AT, Date.from(issue.getUpdatedAt()))
            .put(Schemas.NUMBER, issue.getNumber())
            .put(Schemas.STATE, issue.getState());

        // User is mandatory.
        User user = issue.getUser();

        Struct userStruct = new Struct(Schemas.USER_SCHEMA)
            .put(Schemas.USER_URL, user.getUrl())
            .put(Schemas.USER_ID, user.getId())
            .put(Schemas.USER_LOGIN, user.getLogin());

        valueStruct.put(Schemas.USER, userStruct);

        // Pull request is optional.
        PullRequest pullRequest = issue.getPullRequest();

        if (pullRequest != null) {

            Struct prStruct = new Struct(Schemas.PR_SCHEMA)
                .put(Schemas.PR_URL, pullRequest.getUrl())
                .put(Schemas.PR_HTML_URL, pullRequest.getHtmlUrl());

            valueStruct.put(Schemas.PR, prStruct);
        }
        return valueStruct;
    }
}
