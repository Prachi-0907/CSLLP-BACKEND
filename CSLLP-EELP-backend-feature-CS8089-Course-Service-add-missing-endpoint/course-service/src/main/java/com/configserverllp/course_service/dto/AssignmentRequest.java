package com.configserverllp.course_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentRequest {

    //@NotNull(message = "courseId is required")
    //private Long courseId;

    //@NotNull(message = "employeeId is required")
    //private Long employeeId;

    // LOW, MEDIUM, HIGH - optional
    //private String priority;

//    private String courseName;      // ← NEW
//    private String employeeId;      // ← CHANGE to String (EMP00123)
//    private String employeeName;    // ← NEW (optional, for display)
//
//    // optional
//    private LocalDate dueDate;
//
//    private String notes;
//
//    // the id of user who assigns (admin/manager)
//    @NotNull(message = "assignedBy is required")
//    private Long assignedBy;
@NotBlank(message = "Course name is required")
private String courseName;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private LocalDate dueDate;
    private String notes;

    @NotNull(message = "assignedBy is required")
    private Long assignedBy;
}
