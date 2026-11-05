package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
