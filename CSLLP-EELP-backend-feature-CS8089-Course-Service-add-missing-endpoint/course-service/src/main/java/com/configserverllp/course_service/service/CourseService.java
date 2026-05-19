package com.configserverllp.course_service.service;

import com.configserverllp.course_service.dto.CourseRequest;
import com.configserverllp.course_service.dto.EnrollmentRequest;
import com.configserverllp.course_service.dto.PagedResponse;
import com.configserverllp.course_service.entity.Course;
import com.configserverllp.course_service.entity.Enrollment;


import java.util.List;
import java.util.Map;

public interface CourseService {

    Course createCourse(CourseRequest request);

    Course updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);

    Course getCourseById(Long id);

    List<Course> getAllActiveCourses();

    // 🆕 ADD THIS METHOD - Get ALL courses (both ACTIVE and INACTIVE)
    List<Course> getAllCourses();

    Enrollment enrollCourse(EnrollmentRequest request);

    Enrollment approveEnrollment(Long enrollmentId);

    Enrollment rejectEnrollment(Long enrollmentId);

    List<Enrollment> getEnrollmentsByEmployee(Long employeeId);

    List<Course> getCoursesByCategory(String category);

    List<Course> searchCourses(String keyword);

    List<Course> getCoursesCreatedBy(Long createdBy);

    Enrollment updateProgress(Long enrollmentId, int progress);

    List<Enrollment> getEnrollmentsByCourse(Long courseId);

    List<Map<String, Object>> getMaterialsForCourse(Long courseId);

    boolean checkExamEligibility(Long enrollmentId);

    List<Map<String, Object>> getMyCourses(Long employeeId); // NEW

    List<Enrollment> getPendingEnrollments();

    Map<String, Object> generateCourseReport(); // NEW

    Enrollment incrementProgress(Long enrollmentId, int incrementBy);
    Enrollment markContentComplete(Long enrollmentId, String contentType, String contentTitle);

    // Add to existing interface
    List<Course> searchCoursesByName(String name);

    // 🆕 AUTOMATIC PROGRESS REMINDER METHODS
    void checkAndSendProgressReminders();
    List<Map<String, Object>> getEnrollmentsNeedingReminders();
    void sendAutomaticReminderForEnrollment(Long enrollmentId);

    PagedResponse<Course> getAllActiveCoursePaged(int page, int size, String sort);
    PagedResponse<Course> getAllCoursesPaged(int page, int size, String sort);
    PagedResponse<Course> searchCoursesPaged(String keyword, int page, int size, String sort);
    PagedResponse<Course> getCoursesByCategoryPaged(String category, int page, int size, String sort);
    PagedResponse<Course> getCoursesCreatedByPaged(Long createdBy, int page, int size, String sort);


}