package org.monke.connector.util;

import org.apache.kafka.connect.errors.ConnectException;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for parsing GitHub API's HATEOAS specific "rels" strings from HTTP Link headers.
 */
public class RelsUtils {

    public static String getNextPage(String rels) {
        return getAsMap(rels).get("next");
    }

    private static Map<String, String> getAsMap(String rels) {

        if (rels == null) {
            throw new IllegalArgumentException("Rels string cannot be null");
        }

        try {
            return Arrays.stream(rels.split(","))
                .map(rel -> rel.split(";"))
                .collect(
                    Collectors.toMap(
                        parts -> parts[1].trim().split("=")[1].replaceAll("\"", ""),
                        parts -> parts[0].trim().replaceAll("[<>]", "")
                    )
                );

        } catch (Exception e) {
            throw new ConnectException("Unexpected rels format : " + rels, e);
        }
    }
}
