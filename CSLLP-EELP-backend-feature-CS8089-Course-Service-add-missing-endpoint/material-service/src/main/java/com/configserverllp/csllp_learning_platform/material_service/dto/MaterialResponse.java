package com.configserverllp.csllp_learning_platform.material_service.dto;


import com.configserverllp.csllp_learning_platform.material_service.entity.Material;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String tags;
    private String type;
    private Long uploadedBy;
    private Long courseId;
    private Material.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
