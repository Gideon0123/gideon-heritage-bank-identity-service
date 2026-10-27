package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Invalid email or password");
    }
}