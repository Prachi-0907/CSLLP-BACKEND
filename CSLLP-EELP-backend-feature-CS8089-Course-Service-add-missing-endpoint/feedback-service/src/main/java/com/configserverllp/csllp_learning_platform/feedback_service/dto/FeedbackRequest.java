package com.configserverllp.csllp_learning_platform.feedback_service.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class FeedbackRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String targetType; // COURSE, EXAM, or USER

    @NotNull
    private Long targetId;

    // NEW: For user feedbacks
    private Long receivedByUserId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    private String comments;
}