package com.configserverllp.course_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotNull(message = "CourseId is required")
    private Long courseId;

    @NotNull(message = "EmployeeId is required")
    private Long employeeId;
}
