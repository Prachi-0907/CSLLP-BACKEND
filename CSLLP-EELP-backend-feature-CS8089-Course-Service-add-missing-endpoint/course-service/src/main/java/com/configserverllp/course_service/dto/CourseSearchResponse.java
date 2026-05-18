package com.configserverllp.course_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSearchResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer durationHours;
    private boolean paid;
    private Double price;
}