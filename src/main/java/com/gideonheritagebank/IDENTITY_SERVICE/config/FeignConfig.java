package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.gideonheritagebank.IDENTITY_SERVICE.utils.GatewayHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class FeignConfig {

    private static final String LEGACY_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String LEGACY_ROLES_HEADER = "X-Authenticated-Roles";
    private static final String CORRELATION_ID_PATTERN = "[A-Za-z0-9._:-]{1,128}";

    @Bean
    public RequestInterceptor gatewayIdentityRequestInterceptor() {

        return template -> {

            // Never preserve caller-supplied identity metadata.
            removeIdentityHeaders(template);

            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            if (!(authentication instanceof JwtAuthenticationToken jwtAuth)
                    || !authentication.isAuthenticated()) {

                // No authenticated caller JWT.
                // Leave any independently configured service Authorization
                // header untouched.
                return;
            }

            Jwt jwt = jwtAuth.getToken();

            // Forward the original signed token so that the downstream
            // service can independently validate it.
            template.removeHeader(HttpHeaders.AUTHORIZATION);

            template.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + jwt.getTokenValue()
            );

            setHeader(
                    template,
                    GatewayHeaders.USER_ID,
                    jwt.getSubject()
            );

            setHeader(
                    template,
                    GatewayHeaders.USER_EMAIL,
                    jwt.getClaimAsString("email")
            );

            setHeader(
                    template,
                    GatewayHeaders.USER_ROLE,
                    extractRoles(jwt)
            );

            setHeader(
                    template,
                    GatewayHeaders.CORRELATION_ID,
                    resolveCorrelationId()
            );
        };
    }

    private void removeIdentityHeaders(RequestTemplate template) {

        template.removeHeader(GatewayHeaders.USER_ID);
        template.removeHeader(GatewayHeaders.USER_EMAIL);
        template.removeHeader(GatewayHeaders.USER_ROLE);
        template.removeHeader(GatewayHeaders.CORRELATION_ID);

        // Remove legacy identity headers as well.
        template.removeHeader(LEGACY_USER_ID_HEADER);
        template.removeHeader(LEGACY_ROLES_HEADER);
    }

    private void setHeader(
            RequestTemplate template,
            String name,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            template.removeHeader(name);
            template.header(name, value);
        }
    }

    private String extractRoles(Jwt jwt) {

        Object claim = jwt.getClaims().get("roles");

        if (claim instanceof Collection<?> values) {
            return values.stream()
                    .filter(Objects::nonNull)
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        if (claim instanceof String role && !role.isBlank()) {
            return role;
        }

        return null;
    }

    private String resolveCorrelationId() {

        RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {

            HttpServletRequest request =
                    servletAttributes.getRequest();

            String correlationId =
                    request.getHeader(GatewayHeaders.CORRELATION_ID);

            if (isValidCorrelationId(correlationId)) {
                return correlationId;
            }

            String requestId = request.getHeader("X-Request-Id");

            if (isValidCorrelationId(requestId)) {
                return requestId;
            }
        }

        String mdcCorrelationId = MDC.get("correlationId");

        if (isValidCorrelationId(mdcCorrelationId)) {
            return mdcCorrelationId;
        }

        String traceId = MDC.get("traceId");

        if (isValidCorrelationId(traceId)) {
            return traceId;
        }

        return UUID.randomUUID().toString();
    }

    private boolean isValidCorrelationId(String value) {
        return value != null
                && value.matches(CORRELATION_ID_PATTERN);
    }
}
