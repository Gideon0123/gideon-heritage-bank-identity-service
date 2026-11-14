package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class IdempotencyKeyUtil {

    private static final String PREFIX = "ghb:idempotency";

    private IdempotencyKeyUtil() {
    }

    /**
     * The client-provided key is hashed before being used in Redis. This keeps
     * Redis keys bounded and prevents arbitrary header contents from becoming
     * part of the Redis key namespace.
     */
    public static String buildKey(UUID userId, String idempotencyKey) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to scope an idempotency key.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required.");
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":user:" + userId + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
