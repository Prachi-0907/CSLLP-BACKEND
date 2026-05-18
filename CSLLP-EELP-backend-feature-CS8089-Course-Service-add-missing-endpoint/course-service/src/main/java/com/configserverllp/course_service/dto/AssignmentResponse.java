package com.configserverllp.course_service.dto;

import com.configserverllp.course_service.entity.Assignment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentResponse {
    private Long id;
    private Long courseId;
    private Long employeeId;
   // private String priority;
    private LocalDate dueDate;
    private String notes;
    private Assignment.Status status;
    private Long assignedBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
