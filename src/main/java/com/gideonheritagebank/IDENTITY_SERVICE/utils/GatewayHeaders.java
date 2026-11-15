package com.gideonheritagebank.IDENTITY_SERVICE.utils;

/**
 * Canonical identity and tracing headers used between the API Gateway
 * and trusted internal services.
 * These headers are metadata, not credentials. Downstream services must
 * not authorize a request solely because these headers are present.
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLE = "X-User-Role";
    public static final String CORRELATION_ID = "X-Correlation-ID";

    private GatewayHeaders() {
        throw new UnsupportedOperationException("Utility class");
    }
}
