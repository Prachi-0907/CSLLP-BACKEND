package com.configserverllp.csllp_learning_platform.exam_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceReportDTO {
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String department;
    private Long courseId;
    private String courseName;
    private Integer totalExamsAttempted;
    private Double averageScore;
    private Double bestScore;
    private Double worstScore;
    private Integer totalAttempts;
    private String performanceLevel; // EXCELLENT, GOOD, AVERAGE, POOR
    private LocalDateTime lastAttemptDate;
}