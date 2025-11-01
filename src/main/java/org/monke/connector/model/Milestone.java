package org.monke.connector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Milestone {

    private String url;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("labels_url")
    private String labelsUrl;

    private Integer id;

    private Integer number;

    private String state;

    private String title;

    private String description;

    private Creator creator;

    @JsonProperty("open_issues")
    private Integer openIssues;

    @JsonProperty("closed_issues")
    private Integer closedIssues;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("closed_at")
    private String closedAt;

    @JsonProperty("due_on")
    private String dueOn;

    @JsonProperty("additional_properties")
    private Map<String, Object> additionalProperties = new HashMap<>();
}
