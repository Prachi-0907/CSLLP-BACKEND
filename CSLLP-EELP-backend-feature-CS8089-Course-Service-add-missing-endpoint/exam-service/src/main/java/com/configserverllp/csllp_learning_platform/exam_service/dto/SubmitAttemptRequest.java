package com.configserverllp.csllp_learning_platform.exam_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * For MCQ: answers -> map questionId -> selectedOptionIds

 */
@Data
public class SubmitAttemptRequest {
    @NotNull
    private Long employeeId;

    // generic: questionId -> answer object (string for coding/theory or list of option ids for MCQ)
    // We'll use map<string, object> at JSON level; in service parse as needed.
    private Map<String, Object> answers;
}
