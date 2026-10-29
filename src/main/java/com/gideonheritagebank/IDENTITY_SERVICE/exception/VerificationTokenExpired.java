package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class VerificationTokenExpired extends RuntimeException {
    public VerificationTokenExpired(String message) {
        super(message);
    }
}
