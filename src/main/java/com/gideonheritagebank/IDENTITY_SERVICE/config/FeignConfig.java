package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.gideonheritagebank.IDENTITY_SERVICE.utils.GatewayHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Propagates the verified caller JWT and derived identity metadata to
 * downstream Feign clients. Register this configuration on Feign clients
 * that need caller-context propagation.
 * Identity headers are always derived from the authenticated Jwt; existing
 * caller-supplied X-User-* template headers are removed first.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor gatewayIdentityRequestInterceptor() {
        return template -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                removeIdentityHeaders(template);
                return;
            }

            // Pass the original signed token so the downstream service can
            // independently validate it rather than trusting metadata headers.
            template.removeHeader(HttpHeaders.AUTHORIZATION);
            template.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + jwt.getTokenValue()
            );

            setHeader(template, GatewayHeaders.USER_ID, jwt.getSubject());
            setHeader(template, GatewayHeaders.USER_EMAIL,
                    jwt.getClaimAsString("email"));
            setHeader(template, GatewayHeaders.USER_ROLE, extractRoles(jwt));

            String correlationId = resolveCorrelationId();
            setHeader(template, GatewayHeaders.CORRELATION_ID, correlationId);
        };
    }

    private void removeIdentityHeaders(feign.RequestTemplate template) {
        template.removeHeader(GatewayHeaders.USER_ID);
        template.removeHeader(GatewayHeaders.USER_EMAIL);
        template.removeHeader(GatewayHeaders.USER_ROLE);
    }

    private void setHeader(
            feign.RequestTemplate template,
            String name,
            String value
    ) {
        template.removeHeader(name);
        if (value != null && !value.isBlank()) {
            template.header(name, value);
        }
    }

    private String extractRoles(Jwt jwt) {
        Object claim = jwt.getClaims().get("roles");

        if (claim instanceof Collection<?> values) {
            return values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
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
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();

            String correlationId = request.getHeader(GatewayHeaders.CORRELATION_ID);
            if (correlationId != null && !correlationId.isBlank()) {
                return correlationId;
            }

            String requestId = request.getHeader("X-Request-Id");
            if (requestId != null && !requestId.isBlank()) {
                return requestId;
            }
        }

        String mdcCorrelationId = MDC.get("correlationId");
        return mdcCorrelationId == null || mdcCorrelationId.isBlank()
                ? MDC.get("traceId")
                : mdcCorrelationId;
    }
}