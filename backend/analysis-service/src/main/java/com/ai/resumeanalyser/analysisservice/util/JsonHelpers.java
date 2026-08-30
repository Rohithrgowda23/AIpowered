package com.ai.resumeanalyser.analysisservice.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class JsonHelpers {

    private JsonHelpers() {}

    public static int safeInt(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return 0;
        JsonNode value = node.get(field);
        return value.isNumber() ? value.asInt() : 0;
    }

    public static String safeText(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return "";
        JsonNode value = node.get(field);
        return value.isTextual() ? value.asText() : "";
    }

    public static List<String> safeStringList(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        if (node == null || !node.hasNonNull(field)) return result;
        JsonNode arr = node.get(field);
        if (!arr.isArray()) return result;
        for (JsonNode item : arr) {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText());
            }
        }
        return result;
    }
}
