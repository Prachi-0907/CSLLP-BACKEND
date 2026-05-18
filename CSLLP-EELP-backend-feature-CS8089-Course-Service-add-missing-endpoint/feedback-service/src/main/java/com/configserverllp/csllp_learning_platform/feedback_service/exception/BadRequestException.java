package com.configserverllp.csllp_learning_platform.feedback_service.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
