package com.configserverllp.csllp_learning_platform.exam_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamWithStatus {
    private Long examId;
    private String title;
    private String description;
    private Long courseId;
    private String courseName;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // LOCKED or AVAILABLE
    private String attemptStatus;// NOT_STARTED / IN_PROGRESS / COMPLETED
}
