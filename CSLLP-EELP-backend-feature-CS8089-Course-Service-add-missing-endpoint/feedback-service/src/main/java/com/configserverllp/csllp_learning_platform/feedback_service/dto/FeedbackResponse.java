package com.configserverllp.csllp_learning_platform.feedback_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String userName;      // fetched from UserService
    private String targetType;    // COURSE / EXAM / USER
    private Long targetId;
    private String targetTitle;   // fetched from Course/Exam/User service

    // NEW: For user feedbacks
    private Long receivedByUserId;
    private String receivedByUserName;

    private Integer rating;
    private String comments;
    private String status;
    private LocalDateTime createdAt;
}