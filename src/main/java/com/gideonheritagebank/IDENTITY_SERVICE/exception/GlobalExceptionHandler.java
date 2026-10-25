package com.gideonheritagebank.IDENTITY_SERVICE.exception;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new TreeMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage() != null
                                        ? error.getDefaultMessage() : "Invalid value"
                        )
                );

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new TreeMap<>();

        exception.getConstraintViolations()
                .forEach(violation ->
                        errors.putIfAbsent(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        )
                );

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing, malformed, or contains an invalid value",
                Map.of(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data",
                Map.of(),
                request
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                Map.of(),
                request
        );
    }

    @ExceptionHandler(PhoneNumberAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicatePhoneNumber(
            PhoneNumberAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                Map.of(),
                request
        );
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRegistration(
            InvalidRegistrationException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                Map.of(),
                request
        );
    }

    @ExceptionHandler(RoleConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoleConfiguration(
            RoleConfigurationException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The identity service is not correctly configured",
                Map.of(),
                request
        );
    }

    private ResponseEntity<ApiResponse<Void>> errorResponse(
            HttpStatus status,
            String message,
            Map<String, String> errors,
            HttpServletRequest request
    ) {
        String traceId = TraceIdUtil.generate();
        ApiResponse<Void> body = ApiResponse.failure(
                message,
                errors,
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}