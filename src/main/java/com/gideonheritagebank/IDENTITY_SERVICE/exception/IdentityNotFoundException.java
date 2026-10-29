package com.gideonheritagebank.IDENTITY_SERVICE.exception;

import java.util.UUID;

public class IdentityNotFoundException extends RuntimeException {

    public IdentityNotFoundException(UUID userId) {
        super("Identity not found: " + userId);
    }
}