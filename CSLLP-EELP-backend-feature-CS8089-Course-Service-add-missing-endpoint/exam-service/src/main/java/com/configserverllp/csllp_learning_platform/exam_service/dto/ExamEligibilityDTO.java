package com.configserverllp.csllp_learning_platform.exam_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamEligibilityDTO {
    private Boolean isEligible;
    private String message;
    private Integer courseProgress;
    private Integer requiredProgress;
    private Boolean hasExistingAttempt;
    private String existingAttemptStatus;
}