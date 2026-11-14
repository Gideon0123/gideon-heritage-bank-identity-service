package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RequestFingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class FingerprintService {

    private final ObjectMapper objectMapper;

    public String generate(RequestFingerprint fingerprint) {
        try {
            String normalizedBody = normalizeJson(fingerprint.getRequestBody());
            // Length-prefix each field to avoid ambiguous delimiter combinations.
            String payload = field(fingerprint.getUserId().toString())
                    + field(nullToEmpty(fingerprint.getHttpMethod()))
                    + field(nullToEmpty(fingerprint.getEndpoint()))
                    + field(nullToEmpty(fingerprint.getQueryString()))
                    + field(normalizedBody);

            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate request fingerprint.", exception);
        }
    }

    private String field(String value) {
        return value.length() + ":" + value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeJson(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return "";
        }
        JsonNode parsed = objectMapper.readTree(json);
        return objectMapper.writeValueAsString(sortObjectProperties(parsed));
    }

    private JsonNode sortObjectProperties(JsonNode node) {
        if (node == null || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode sortedArray = objectMapper.createArrayNode();
            node.forEach(value -> sortedArray.add(sortObjectProperties(value)));
            return sortedArray;
        }
        if (node.isObject()) {
            ObjectNode sortedObject = objectMapper.createObjectNode();
            Map<String, JsonNode> sortedFields = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                sortedFields.put(entry.getKey(), entry.getValue());
            }
            sortedFields.forEach((name, value) ->
                    sortedObject.set(name, sortObjectProperties(value)));
            return sortedObject;
        }
        return node;
    }
}
