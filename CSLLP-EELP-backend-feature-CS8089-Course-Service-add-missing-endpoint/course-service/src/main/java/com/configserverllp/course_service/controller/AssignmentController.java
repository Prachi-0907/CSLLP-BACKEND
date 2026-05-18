package com.configserverllp.course_service.controller;

import com.configserverllp.course_service.dto.AssignmentRequest;
import com.configserverllp.course_service.dto.BulkAssignmentRequest;
import com.configserverllp.course_service.dto.BulkAssignmentResponse;
import com.configserverllp.course_service.dto.EmployeeSearchResponse;
import com.configserverllp.course_service.entity.Assignment;
import com.configserverllp.course_service.service.AssignmentService;
import com.configserverllp.course_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ========== SINGLE ASSIGNMENT (UPDATED) ==========
    @PostMapping
    public ResponseEntity<ApiResponse<Assignment>> create(@RequestBody Assignment assignment) {
        return ResponseEntity.ok(ApiResponse.success("Assignment created", assignmentService.createAssignment(assignment)));
    }

    // 🆕 NEW ENDPOINT: Search employees by name/ID
    @GetMapping("/search/employees")
    public ResponseEntity<ApiResponse<List<EmployeeSearchResponse>>> searchEmployees(
            @RequestParam String query) {
        List<EmployeeSearchResponse> employees = assignmentService.searchEmployees(query);
        return ResponseEntity.ok(ApiResponse.success("Employees found", employees));
    }

    // 🆕 NEW ENDPOINT: Bulk assignment with names
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkAssignmentResponse>> createBulkAssignment(
            @RequestBody BulkAssignmentRequest request) {
        BulkAssignmentResponse response = assignmentService.createBulkAssignment(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk assignment processed", response));
    }


//    @GetMapping("/search/courses")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchCourses(@RequestParam String query) {
//        List<Map<String, Object>> results = assignmentService.searchCourses(query);
//        return ResponseEntity.ok(ApiResponse.success("Courses found", results));
//    }

    // ========== EXISTING ENDPOINTS (UNCHANGED) ==========
    @GetMapping
    public ResponseEntity<ApiResponse<List<Assignment>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long courseId) {

        Map<String, String> filters = new HashMap<>();
        if (status != null) filters.put("status", status);
        if (employeeId != null) filters.put("employeeId", employeeId.toString());
        if (courseId != null) filters.put("courseId", courseId.toString());

        List<Assignment> assignments = assignmentService.listAssignments(filters);
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched successfully", assignments));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Assignment>>> pending() {
        List<Assignment> pendingAssignments = assignmentService.getPendingAssignments();
        return ResponseEntity.ok(ApiResponse.success("Pending assignments fetched", pendingAssignments));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Assignment>> approve(
            @PathVariable Long id,
            @RequestParam Long approverId) {

        Assignment approvedAssignment = assignmentService.approveAssignment(id, approverId);
        return ResponseEntity.ok(ApiResponse.success("Assignment approved successfully", approvedAssignment));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Assignment>> reject(
            @PathVariable Long id,
            @RequestParam Long approverId) {

        Assignment rejectedAssignment = assignmentService.rejectAssignment(id, approverId);
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected", rejectedAssignment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Assignment>> getById(@PathVariable Long id) {
        Assignment assignment = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Assignment fetched successfully", assignment));
    }
}