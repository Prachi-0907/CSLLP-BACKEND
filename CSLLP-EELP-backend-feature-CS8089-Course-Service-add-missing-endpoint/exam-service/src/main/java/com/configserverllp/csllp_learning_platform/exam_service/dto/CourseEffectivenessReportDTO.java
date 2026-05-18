package com.configserverllp.csllp_learning_platform.exam_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEffectivenessReportDTO {
    private Long courseId;
    private String courseName;
    private Integer totalEnrollments;
    private Integer totalExamAttempts;
    private Double averageScore;
    private Double passRate;
    private Double completionRate;
    private Integer totalQuestions;
    private String difficultyLevel; // EASY, MEDIUM, HARD
    private String effectivenessRating; // HIGH, MEDIUM, LOW
}