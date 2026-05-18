package com.configserverllp.course_service.dto;

import lombok.Data;

@Data
public class EmployeeAssignment {
    private Long id;           // Internal user ID
    private String employeeId; // EMP00123 format
    private String name;       // Full name
    private String email;      // Email for verification
    private String department; // Department for display
}