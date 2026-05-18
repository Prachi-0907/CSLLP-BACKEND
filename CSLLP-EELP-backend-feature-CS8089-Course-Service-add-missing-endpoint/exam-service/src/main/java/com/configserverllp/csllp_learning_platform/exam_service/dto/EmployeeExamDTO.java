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
public class EmployeeExamDTO {
    private Long examId;
    private String title;
    private String description;
    private Long courseId;
    private String courseName;
    private Integer durationMinutes;
    //private LocalDateTime startTime;
   // private LocalDateTime endTime;
    private String status; // LOCKED or AVAILABLE
    private String attemptStatus; // NOT_STARTED / IN_PROGRESS / COMPLETED
    private Integer courseProgress;
    private Boolean isEligible;
    private String eligibilityMessage;
}