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
public class Label {

    private Integer id;

    private String url;

    private String name;

    private String color;

    @JsonProperty("default")
    private Boolean _default;

    @JsonProperty("additional_properties")
    private Map<String, Object> additionalProperties = new HashMap<>();
}
