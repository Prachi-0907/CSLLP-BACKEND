package com.configserverllp.course_service.dto;
import com.configserverllp.course_service.entity.Course;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

@Data
public class CourseRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String category;

    @NotNull(message = "Duration is required")
    private Integer durationHours;

    private boolean paid; // lowercase

    private Double price;

    @NotNull(message = "CreatedBy is required")
    private Long createdBy;

    // ✅ New field to pass Material IDs
    private List<Long> materials;


    // ✅ FIXED: Use proper field name
    @JsonProperty("isMandatory")
    private boolean mandatory = false;

    // ✅ ADD: Helper method for entity building
    public boolean isMandatory() {
        return mandatory;
    }
    private Course.Status status;

    public Course.Status getStatus() {
        return status;
    }

    public void setStatus(Course.Status status) {
        this.status = status;
    }


}
