package com.configserverllp.csllp_learning_platform.material_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaterialRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    // for manual metadata creation this can be provided (e.g. external link)
    private String fileUrl;

    private String tags;

    // MaterialRequest.java
    private String type;

    @NotNull(message = "Uploader ID is required")
    private Long uploadedBy;


    private Long courseId;

}
