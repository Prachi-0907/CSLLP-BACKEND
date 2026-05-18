package com.configserverllp.course_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignmentResponse {
    private int totalRequestedAssignments;
    private int successfulAssignments;
    private int failedAssignments;
    private List<AssignmentError> errors;
    private String summary;
}