package com.configserverllp.csllp_learning_platform.certification_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CertificationResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long courseId;
    private String courseName;
    private String courseDescription;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String status; // ACTIVE, EXPIRED, REVOKED
    private String certificateUrl;
    private String verificationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}