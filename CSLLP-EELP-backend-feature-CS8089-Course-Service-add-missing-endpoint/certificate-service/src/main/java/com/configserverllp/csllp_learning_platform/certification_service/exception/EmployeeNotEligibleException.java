package com.configserverllp.csllp_learning_platform.certification_service.exception;

public class EmployeeNotEligibleException extends RuntimeException {
    public EmployeeNotEligibleException(String message) {
        super(message);
    }

    public EmployeeNotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}