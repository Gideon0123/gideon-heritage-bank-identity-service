package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email address already exists");
    }
}