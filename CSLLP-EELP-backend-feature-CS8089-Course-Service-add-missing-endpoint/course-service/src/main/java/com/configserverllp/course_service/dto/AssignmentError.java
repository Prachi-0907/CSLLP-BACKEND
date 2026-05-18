package com.configserverllp.course_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignmentError {
    private String employeeId;
    private String employeeName;
    private String courseName;
    private String errorMessage;
}