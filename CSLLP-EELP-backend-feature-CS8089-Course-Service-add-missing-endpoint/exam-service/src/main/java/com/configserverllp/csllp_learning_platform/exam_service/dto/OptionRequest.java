package com.configserverllp.csllp_learning_platform.exam_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptionRequest {
    @NotBlank
    private String text;

    @JsonProperty("isCorrect")
    private boolean isCorrect;
}
