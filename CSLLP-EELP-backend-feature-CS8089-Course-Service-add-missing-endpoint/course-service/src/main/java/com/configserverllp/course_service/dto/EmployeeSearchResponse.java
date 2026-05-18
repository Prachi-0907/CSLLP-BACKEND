package com.configserverllp.course_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSearchResponse {
    private Long id;
    private String employeeId; // EMP00123 format
    private String name;
    private String email;
    private String department;
    private String role;
    private String status;
}