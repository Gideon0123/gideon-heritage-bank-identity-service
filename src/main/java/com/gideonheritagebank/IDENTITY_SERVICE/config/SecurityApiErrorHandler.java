package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SecurityApiErrorHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityApiErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        writeError(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required or the access token is invalid."
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException, ServletException {

        if (exception instanceof CsrfException) {
            writeError(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "CSRF_VALIDATION_FAILED",
                    "CSRF validation failed. Obtain a fresh CSRF token and send it in the X-XSRF-TOKEN header with the matching XSRF-TOKEN cookie."
            );

            return;
        }

        writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this operation."
        );
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        String traceId = request.getHeader("X-Request-Id");

        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdUtil.generate();
        }

        ApiResponse<Void> body = ApiResponse.failure(
                message,
                status.value(),
                Map.of("code", errorCode),
                request.getRequestURI(),
                traceId
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", traceId);
        response.setHeader("Cache-Control", "no-store");

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}