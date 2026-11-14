package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class IdempotencyProcessingException extends RuntimeException {
    public IdempotencyProcessingException(String message) {
        super(message);
    }
}
