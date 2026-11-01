package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
