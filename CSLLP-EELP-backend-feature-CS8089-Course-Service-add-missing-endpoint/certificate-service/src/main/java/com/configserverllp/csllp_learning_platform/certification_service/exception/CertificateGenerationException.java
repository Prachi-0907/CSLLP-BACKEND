package com.configserverllp.csllp_learning_platform.certification_service.exception;

public class CertificateGenerationException extends RuntimeException {
    public CertificateGenerationException(String message) {
        super(message);
    }

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}