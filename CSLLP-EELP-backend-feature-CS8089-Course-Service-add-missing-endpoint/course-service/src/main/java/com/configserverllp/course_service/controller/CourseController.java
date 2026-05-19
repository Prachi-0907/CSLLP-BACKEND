package com.configserverllp.course_service.controller;

import com.configserverllp.course_service.dto.CourseRequest;
import com.configserverllp.course_service.dto.CourseSearchResponse;
import com.configserverllp.course_service.dto.EnrollmentRequest;
import com.configserverllp.course_service.dto.PagedResponse;
import com.configserverllp.course_service.entity.Course;
import com.configserverllp.course_service.entity.Enrollment;
import com.configserverllp.course_service.service.CourseService;
import com.configserverllp.course_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;


    @PostMapping
    public ResponseEntity<ApiResponse<Course>> create(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course created", courseService.createCourse(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course updated", courseService.updateCourse(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Course fetched", courseService.getCourseById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Courses fetched", courseService.getAllActiveCourses()));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<Course>>>getAllCoursesForAdmin(){
        return ResponseEntity.ok(ApiResponse.success("All courses fetched", courseService.getAllCourses()));
    }

    @PostMapping("/enroll")
    public ResponseEntity<ApiResponse<Enrollment>> enroll(@Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment submitted", courseService.enrollCourse(request)));
    }

    @PutMapping("/enrollments/{id}/approve")
    public ResponseEntity<ApiResponse<Enrollment>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment approved", courseService.approveEnrollment(id)));
    }

    @PutMapping("/enrollments/{id}/reject")
    public ResponseEntity<ApiResponse<Enrollment>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment rejected", courseService.rejectEnrollment(id)));
    }

    @GetMapping("/enrollments/{employeeId}")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollments(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Enrollments fetched", courseService.getEnrollmentsByEmployee(employeeId)));
    }



    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Course>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success("Courses by category fetched", courseService.getCoursesByCategory(category)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Course>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Courses search results", courseService.searchCourses(keyword)));
    }

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<ApiResponse<List<Course>>> getCreatedBy(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Courses created by user fetched", courseService.getCoursesCreatedBy(userId)));
    }

    @PutMapping("/enrollments/{id}/progress")
    public ResponseEntity<ApiResponse<Enrollment>> updateProgress(@PathVariable Long id, @RequestParam int progress) {
        return ResponseEntity.ok(ApiResponse.success("Progress updated", courseService.updateProgress(id, progress)));
    }

    @GetMapping("/{courseId}/enrollments")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getEnrollmentsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Enrollments by course fetched", courseService.getEnrollmentsByCourse(courseId)));
    }

    @GetMapping("/{courseId}/materials")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourseMaterials(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Materials fetched for course " + courseId,
                courseService.getMaterialsForCourse(courseId)
        ));
    }
    @GetMapping("/enrollments/{id}/exam-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkExamEligibility(@PathVariable Long id) {
        boolean eligible = courseService.checkExamEligibility(id);
        String msg = eligible ? "Eligible for exam" : "Not eligible - complete the course first.";
        return ResponseEntity.ok(ApiResponse.success(msg, eligible));
    }

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyCourses(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("My courses fetched", courseService.getMyCourses(employeeId)));
    }

    @GetMapping("/enrollments/pending")
    public ResponseEntity<ApiResponse<List<Enrollment>>> getPendingEnrollments() {
        return ResponseEntity.ok(ApiResponse.success("Pending enrollments fetched", courseService.getPendingEnrollments()));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport() {
        return ResponseEntity.ok(ApiResponse.success("Course report generated", courseService.generateCourseReport()));
    }
    @PutMapping("/enrollments/{id}/increment-progress")
    public ResponseEntity<ApiResponse<Enrollment>> incrementProgress(
            @PathVariable Long id,
            @RequestParam int incrementBy) {
        return Optional.of(incrementBy)
                .map(inc -> courseService.incrementProgress(id, inc))
                .map(enrollment -> ApiResponse.success("Progress incremented by " + incrementBy + "%", enrollment))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * Mark specific content as completed - for manual "Mark Complete" buttons
     */
    @PutMapping("/enrollments/{id}/mark-complete")
    public ResponseEntity<ApiResponse<Enrollment>> markContentComplete(
            @PathVariable Long id,
            @RequestParam String contentType,
            @RequestParam String contentTitle) {
        return Optional.of(contentType)
                .map(type -> courseService.markContentComplete(id, type, contentTitle))
                .map(enrollment -> ApiResponse.success(contentTitle + " marked as completed", enrollment))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }
    @GetMapping("/search-by-name")
    public ResponseEntity<ApiResponse<List<CourseSearchResponse>>> searchCoursesByName(
            @RequestParam String name) {
        List<CourseSearchResponse> results = courseService.searchCoursesByName(name)
                .stream()
                .map(course -> CourseSearchResponse.builder()
                        .id(course.getId())
                        .title(course.getTitle())
                        .description(course.getDescription())
                        .category(course.getCategory())
                        .durationHours(course.getDurationHours())
                        .paid(course.isPaid())
                        .price(course.getPrice())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Courses found", results));
    }

    // 🆕 AUTOMATIC PROGRESS REMINDER ENDPOINTS - Add these to CourseController

    @GetMapping("/admin/reminders/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingReminders() {
        try {
            List<Map<String, Object>> pendingEnrollments = courseService.getEnrollmentsNeedingReminders();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalPending", pendingEnrollments.size());
            summary.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success(
                    "Pending reminders fetched successfully",
                    pendingEnrollments
            ));

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch pending reminders: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/reminders/send-automatic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendAutomaticReminders() {
        try {
            courseService.checkAndSendProgressReminders();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Automatic progress reminders sent successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success("Reminders sent successfully", response));

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send automatic reminders: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/reminders/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReminderSystemStatus() {
        try {
            List<Map<String, Object>> pendingReminders = courseService.getEnrollmentsNeedingReminders();

            Map<String, Object> status = new HashMap<>();
            status.put("systemStatus", "ACTIVE");
            status.put("lastRun", LocalDateTime.now());
            status.put("nextRun", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0));
            status.put("totalPendingReminders", pendingReminders.size());
            status.put("schedule", "Daily at 9:00 AM");
            status.put("criteria", "Enrollments >15 days old with progress <50%");

            return ResponseEntity.ok(ApiResponse.success("Reminder system status", status));

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get system status: " + e.getMessage()));
        }
    }

    // Course Catalog - Active courses with pagination (Employee/Manager view)
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PagedResponse<Course>>> getAllActiveCoursePaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success("Courses fetched",
                courseService.getAllActiveCoursePaged(page, size, sort)));
    }

    // Admin - All courses with pagination
    @GetMapping("/admin/all/paged")
    public ResponseEntity<ApiResponse<PagedResponse<Course>>> getAllCoursesForAdminPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success("All courses fetched",
                courseService.getAllCoursesPaged(page, size, sort)));
    }

    // Search with pagination
    @GetMapping("/search/paged")
    public ResponseEntity<ApiResponse<PagedResponse<Course>>> searchCoursesPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
                courseService.searchCoursesPaged(keyword, page, size, sort)));
    }

    // By category with pagination
    @GetMapping("/category/{category}/paged")
    public ResponseEntity<ApiResponse<PagedResponse<Course>>> getCoursesByCategoryPaged(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success("Courses by category fetched",
                courseService.getCoursesByCategoryPaged(category, page, size, sort)));
    }

    // Created by user with pagination (Manager's courses tab)
    @GetMapping("/created-by/{userId}/paged")
    public ResponseEntity<ApiResponse<PagedResponse<Course>>> getCreatedByPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.success("Courses created by user fetched",
                courseService.getCoursesCreatedByPaged(userId, page, size, sort)));
    }
}