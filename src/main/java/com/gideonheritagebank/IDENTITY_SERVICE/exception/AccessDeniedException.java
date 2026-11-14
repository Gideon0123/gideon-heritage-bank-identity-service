package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
