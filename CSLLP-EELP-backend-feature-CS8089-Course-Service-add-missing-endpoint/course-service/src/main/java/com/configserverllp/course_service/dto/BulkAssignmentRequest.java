package com.configserverllp.course_service.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class BulkAssignmentRequest {

    @NotNull(message = "Employees list is required")
    private List<EmployeeAssignment> employees;

    @NotNull(message = "Course names list is required")
    private List<String> courseNames;

    private LocalDate dueDate;
    private String notes;

    @NotNull(message = "Assigned by user ID is required")
    private Long assignedBy;
}