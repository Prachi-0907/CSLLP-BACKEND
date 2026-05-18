package com.configserverllp.csllp_learning_platform.exam_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    @NotBlank(message = "questionText is required")
    private String questionText;

    @NotBlank(message = "questionType required (MCQ/THEORETICAL/CODING)")
    private String questionType;

    // for MCQ
    private List<OptionRequest> options;


    // weight / marks
    @NotNull(message = "marks required")
    @Min(1)
    private Integer marks;
}
