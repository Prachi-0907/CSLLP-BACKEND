package com.configserverllp.csllp_learning_platform.submission_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SubmissionDto {
    private Long id;
    private Long examId;
    private Long userId;
    private String answer;
    private Double marks;
    private LocalDateTime submittedAt;
}
