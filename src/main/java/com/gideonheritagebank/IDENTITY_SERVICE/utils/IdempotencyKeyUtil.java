package com.gideonheritagebank.IDENTITY_SERVICE.utils;

public final class IdempotencyKeyUtil {

    private static final String PREFIX = "idempotency";

    private IdempotencyKeyUtil() {
    }

    public static String buildKey(Long userId, String idempotencyKey) {
        return String.format(
                "%s:user:%d:%s",
                PREFIX,
                userId,
                idempotencyKey
        );
    }
}