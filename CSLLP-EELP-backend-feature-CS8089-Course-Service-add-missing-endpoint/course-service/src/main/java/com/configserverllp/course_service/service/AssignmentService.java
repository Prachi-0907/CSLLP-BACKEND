package com.configserverllp.course_service.service;

import com.configserverllp.course_service.dto.AssignmentRequest;
import com.configserverllp.course_service.dto.BulkAssignmentRequest;
import com.configserverllp.course_service.dto.BulkAssignmentResponse;
import com.configserverllp.course_service.dto.EmployeeSearchResponse;
import com.configserverllp.course_service.entity.Assignment;

import java.util.List;
import java.util.Map;

public interface AssignmentService {

    Assignment createAssignment(Assignment assignment);

    Assignment approveAssignment(Long assignmentId, Long approverId);

    Assignment rejectAssignment(Long assignmentId, Long approverId);

    Assignment getAssignmentById(Long id);

    List<Assignment> listAssignments(Map<String, String> filters);

    List<Assignment> getPendingAssignments();

    // 🆕 NEW METHODS FOR BULK ASSIGNMENTS
    List<EmployeeSearchResponse> searchEmployees(String query);
    BulkAssignmentResponse createBulkAssignment(BulkAssignmentRequest request);
}