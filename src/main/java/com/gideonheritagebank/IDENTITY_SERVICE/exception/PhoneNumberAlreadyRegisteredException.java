package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class PhoneNumberAlreadyRegisteredException extends RuntimeException {

    public PhoneNumberAlreadyRegisteredException() {
        super("An account with this phone number already exists");
    }
}
