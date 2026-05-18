package com.configserverllp.csllp_learning_platform.exam_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ExamRequest {

    @NotBlank(message = "Title required")
    private String title;

    private String description;

    @NotNull(message = "Duration required")
    @Min(value = 1, message = "Duration must be >= 1")
    private Integer durationMinutes;

    @NotNull(message = "createdBy required")
    private Long createdBy;

    @NotNull(message = "courseId required")
    private Long courseId;

    @NotNull(message = "examType required")
    private String examType;
}