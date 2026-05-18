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
public class EmployeeResultDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Long courseId;
    private String courseName;
    private Double score;
    private Integer totalMarks;
    private Double percentage;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String feedback;
    private String grade;
    private Integer timeTakenMinutes;
}