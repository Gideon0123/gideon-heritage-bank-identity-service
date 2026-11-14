package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RequestFingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class FingerprintService {

    private final ObjectMapper objectMapper;

    public String generate(RequestFingerprint fingerprint) {

        try {
            String normalizedBody = normalizeJson(
                    fingerprint.getRequestBody()
            );

            String payload = fingerprint.getUserId()
                    + "|"
                    + fingerprint.getHttpMethod()
                    + "|"
                    + fingerprint.getEndpoint()
                    + "|"
                    + normalizedBody;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }

        catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to generate request fingerprint",
                    ex
            );

        }

    }

    private String normalizeJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return "";
            }
            JsonNode node = objectMapper.readTree(json);

            return objectMapper.writeValueAsString(node);
        }

        catch (Exception ex) {
            return json;
        }
    }
}
