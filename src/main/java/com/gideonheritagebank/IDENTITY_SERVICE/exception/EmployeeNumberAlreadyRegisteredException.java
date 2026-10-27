package com.gideonheritagebank.IDENTITY_SERVICE.exception;

public class EmployeeNumberAlreadyRegisteredException extends RuntimeException {

    public EmployeeNumberAlreadyRegisteredException() {
        super("An employee with this employee number already exists");
    }
}
