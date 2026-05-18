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
public class ExamResultDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long courseId;
    private String courseName;
    private Double score;
    private Integer totalMarks;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String feedback;
    private Integer timeTakenMinutes;

    // ✅ New fields
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Double percentage;
    private String grade;
}