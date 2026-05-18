package com.configserverllp.csllp_learning_platform.certification_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CertificationRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String additionalNotes;
}