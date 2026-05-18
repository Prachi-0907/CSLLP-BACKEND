package com.configserverllp.course_service.service.impl;

import com.configserverllp.course_service.dto.*;
import com.configserverllp.course_service.entity.Assignment;
import com.configserverllp.course_service.entity.Course;
import com.configserverllp.course_service.entity.Enrollment;
import com.configserverllp.course_service.exception.BadRequestException;
import com.configserverllp.course_service.exception.ResourceNotFoundException;
import com.configserverllp.course_service.repository.AssignmentRepository;
import com.configserverllp.course_service.repository.CourseRepository;
import com.configserverllp.course_service.repository.EnrollmentRepository;
import com.configserverllp.course_service.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate;

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    // 🆕 NEW METHOD: Search employees from User Service
    @Override
    public List<EmployeeSearchResponse> searchEmployees(String query) {
        try {
            String url = userServiceBaseUrl + "/api/users/search?query=" + query;

            // Call User Service to search employees
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return Collections.emptyList();
            }

            // Extract employee data from response
            List<Map<String, Object>> employeesData = (List<Map<String, Object>>) response.get("data");

            return employeesData.stream()
                    .map(this::mapToEmployeeSearchResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // 🆕 NEW METHOD: Create bulk assignments
    @Override
    @Transactional
    public BulkAssignmentResponse createBulkAssignment(BulkAssignmentRequest request) {
        List<AssignmentError> errors = new ArrayList<>();
        int successfulAssignments = 0;
        int totalRequested = request.getEmployees().size() * request.getCourseNames().size();

        // Process each employee and course combination
        for (EmployeeAssignment employee : request.getEmployees()) {
            for (String courseName : request.getCourseNames()) {
                try {
                    // Resolve course name to course ID
                    Course course = resolveCourseByName(courseName);

                    // Check if assignment already exists
                    if (isAssignmentExists(employee.getId(), course.getId())) {
                        errors.add(new AssignmentError(
                                employee.getEmployeeId(),
                                employee.getName(),
                                courseName,
                                "Already assigned to this course"
                        ));
                        continue;
                    }

                    // Create the assignment
                    createSingleAssignment(employee.getId(), course.getId(), request);
                    successfulAssignments++;

                } catch (Exception e) {
                    errors.add(new AssignmentError(
                            employee.getEmployeeId(),
                            employee.getName(),
                            courseName,
                            e.getMessage()
                    ));
                }
            }
        }

        // Build response
        return BulkAssignmentResponse.builder()
                .totalRequestedAssignments(totalRequested)
                .successfulAssignments(successfulAssignments)
                .failedAssignments(errors.size())
                .errors(errors)
                .summary(String.format("Successfully created %d out of %d assignments",
                        successfulAssignments, totalRequested))
                .build();
    }

    // 🆕 HELPER: Map user service response to EmployeeSearchResponse
    private EmployeeSearchResponse mapToEmployeeSearchResponse(Map<String, Object> userData) {
        return EmployeeSearchResponse.builder()
                .id(((Number) userData.get("id")).longValue())
                .employeeId("EMP" + String.format("%05d", ((Number) userData.get("id")).longValue()))
                .name((String) userData.get("firstName") + " " + (String) userData.get("lastName"))
                .email((String) userData.get("email"))
                .department((String) userData.get("department"))
                .role((String) userData.get("role"))
                .status((String) userData.get("status"))
                .build();
    }

    // 🆕 HELPER: Resolve course name to Course entity
    private Course resolveCourseByName(String courseName) {
        List<Course> courses = courseRepository.findAll().stream()
                .filter(course -> course.getTitle().equalsIgnoreCase(courseName) &&
                        course.getStatus() == Course.Status.ACTIVE)
                .collect(Collectors.toList());

        if (courses.isEmpty()) {
            throw new BadRequestException("Course not found: " + courseName);
        }
        if (courses.size() > 1) {
            throw new BadRequestException("Multiple courses found with name: " + courseName);
        }

        return courses.get(0);
    }

    // 🆕 HELPER: Check if assignment already exists
    private boolean isAssignmentExists(Long employeeId, Long courseId) {
        return assignmentRepository.findByEmployeeIdAndCourseId(employeeId, courseId)
                .stream()
                .anyMatch(assignment -> assignment.getStatus() != Assignment.Status.REJECTED);
    }

    // 🆕 HELPER: Create single assignment
    private void createSingleAssignment(Long employeeId, Long courseId, BulkAssignmentRequest request) {
        Assignment assignment = Assignment.builder()
                .employeeId(employeeId)
                .courseId(courseId)
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .assignedBy(request.getAssignedBy())
                .status(Assignment.Status.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);

        // Also create enrollment record
        try {
            Enrollment enrollment = Enrollment.builder()
                    .courseId(courseId)
                    .employeeId(employeeId)
                    .status(Enrollment.Status.APPROVED)
                    .progress(0)
                    .enrolledAt(LocalDateTime.now())
                    .build();
            enrollmentRepository.save(enrollment);
        } catch (Exception e) {
            // Log but don't fail the assignment if enrollment fails
            System.err.println("Failed to create enrollment for employee " + employeeId + " in course " + courseId);
        }
    }

    // ✅ EXISTING METHODS (NO CHANGES)
    @Override
    public Assignment createAssignment(Assignment assignment) {
        // validate course exists
        Course course = courseRepository.findById(assignment.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + assignment.getCourseId()));

        // 🆕 AUTO-APPROVE ADMIN ASSIGNMENTS
        assignment.setStatus(Assignment.Status.APPROVED);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        Assignment savedAssignment = assignmentRepository.save(assignment);

        // 🆕 CREATE ENROLLMENT RECORD SO EMPLOYEE SEES IT IN "MY COURSES"
        try {
            Enrollment enrollment = Enrollment.builder()
                    .courseId(assignment.getCourseId())
                    .employeeId(assignment.getEmployeeId())
                    .status(Enrollment.Status.APPROVED) // Auto-approve since admin assigned it
                    .progress(0)
                    .enrolledAt(LocalDateTime.now())
                    .build();

            enrollmentRepository.save(enrollment);
            System.out.println("✅ Created enrollment for employee: " + assignment.getEmployeeId() + " in course: " + assignment.getCourseId());
        } catch (Exception e) {
            System.err.println("❌ Failed to create enrollment: " + e.getMessage());
            // Don't throw - we don't want assignment to fail if enrollment creation fails
        }

        return savedAssignment;
    }

    @Override
    public Assignment approveAssignment(Long assignmentId, Long approverId) {
        Assignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (a.getStatus() != Assignment.Status.PENDING) {
            throw new BadRequestException("Only pending assignments can be approved");
        }
        a.setStatus(Assignment.Status.APPROVED);
        a.setApprovedBy(approverId);
        a.setApprovedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return assignmentRepository.save(a);
    }

    @Override
    public Assignment rejectAssignment(Long assignmentId, Long approverId) {
        Assignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (a.getStatus() != Assignment.Status.PENDING) {
            throw new BadRequestException("Only pending assignments can be rejected");
        }
        a.setStatus(Assignment.Status.REJECTED);
        a.setRejectedBy(approverId);
        a.setRejectedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return assignmentRepository.save(a);
    }

    @Override
    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }

    @Override
    public List<Assignment> listAssignments(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return assignmentRepository.findAll();
        }

        if (filters.containsKey("status")) {
            Assignment.Status status = Assignment.Status.valueOf(filters.get("status"));
            if (filters.containsKey("employeeId")) {
                Long empId = Long.parseLong(filters.get("employeeId"));
                return assignmentRepository.findByEmployeeIdAndStatus(empId, status);
            }
            if (filters.containsKey("courseId")) {
                Long courseId = Long.parseLong(filters.get("courseId"));
                return assignmentRepository.findByCourseIdAndStatus(courseId, status);
            }
            return assignmentRepository.findByStatus(status);
        }

        if (filters.containsKey("employeeId")) {
            Long empId = Long.parseLong(filters.get("employeeId"));
            return assignmentRepository.findByEmployeeId(empId);
        }

        if (filters.containsKey("courseId")) {
            Long courseId = Long.parseLong(filters.get("courseId"));
            return assignmentRepository.findByCourseId(courseId);
        }

        return assignmentRepository.findAll();
    }

    @Override
    public List<Assignment> getPendingAssignments() {
        return assignmentRepository.findByStatus(Assignment.Status.PENDING);
    }
}