package com.configserverllp.csllp_learning_platform.certification_service.exception;

import com.configserverllp.csllp_learning_platform.certification_service.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.errorWithData(
                "Validation failed", errors);

        log.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(response);
    }

    // Handle bad requests
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());

        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    // Handle resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle certificate generation errors
    @ExceptionHandler(CertificateGenerationException.class)
    public ResponseEntity<ApiResponse<String>> handleCertificateGenerationException(
            CertificateGenerationException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error("Certificate generation failed: " + ex.getMessage());

        log.error("Certificate generation error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle unauthorized access
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());

        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Handle employee not eligible
    @ExceptionHandler(EmployeeNotEligibleException.class)
    public ResponseEntity<ApiResponse<String>> handleEmployeeNotEligibleException(
            EmployeeNotEligibleException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());

        log.warn("Employee not eligible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGlobalException(
            Exception ex, WebRequest request) {

        // Log the full error for debugging
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        // Return a generic error message to the client
        ApiResponse<String> response = ApiResponse.error(
                "An unexpected error occurred. Please try again later.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle file operation exceptions
    @ExceptionHandler(java.nio.file.FileSystemException.class)
    public ResponseEntity<ApiResponse<String>> handleFileSystemException(
            java.nio.file.FileSystemException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(
                "File operation failed: " + ex.getMessage());

        log.error("File system error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle IO exceptions
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiResponse<String>> handleIOException(
            java.io.IOException ex, WebRequest request) {

        ApiResponse<String> response = ApiResponse.error(
                "IO operation failed: " + ex.getMessage());

        log.error("IO error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}