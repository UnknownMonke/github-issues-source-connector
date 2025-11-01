package org.monke.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {

    private Integer id;

    private String url;

    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("labels_url")
    private String labelsUrl;

    @JsonProperty("comments_url")
    private String commentsUrl;

    @JsonProperty("events_url")
    private String eventsUrl;

    @JsonProperty("html_url")
    private String htmlUrl;

    private Integer number;

    private String state;

    private String title;

    private String body;

    private User user;

    private List<Label> labels = null;

    private Assignee assignee;

    private Milestone milestone;

    private Boolean locked;

    private Integer comments;

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    @JsonProperty("closed_at")
    private Object closedAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    private List<Assignee> assignees = null;

    @JsonProperty("additional_properties")
    private Map<String, Object> additionalProperties = new HashMap<>();
}
