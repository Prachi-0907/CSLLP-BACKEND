package com.configserverllp.course_service.service.impl;

import com.configserverllp.course_service.dto.CourseRequest;
import com.configserverllp.course_service.dto.EnrollmentRequest;
import com.configserverllp.course_service.entity.Course;
import com.configserverllp.course_service.entity.Enrollment;
import com.configserverllp.course_service.exception.BadRequestException;
import com.configserverllp.course_service.exception.ResourceNotFoundException;
import com.configserverllp.course_service.repository.CourseRepository;
import com.configserverllp.course_service.repository.EnrollmentRepository;
import com.configserverllp.course_service.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate;

    // injected from application.properties
    @Value("${notification.service.url}")
    private String notificationServiceUrl;   // e.g. http://localhost:8089/api/notifications/send-email

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;       // e.g. http://localhost:8081

    @Value("${material.service.base-url}")
    private String materialServiceBaseUrl;


    @Override
    @Transactional
    public Course createCourse(CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .durationHours(request.getDurationHours())
                .paid(request.isPaid())  // lowercase, fixed
                .price(request.getPrice())
                .createdBy(request.getCreatedBy())
                .status(Course.Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .materialIds(request.getMaterials()) // ✅ store material IDs
                .mandatory(request.isMandatory())
                .build();


        Course saved = courseRepository.save(course);

        if (saved.isMandatory()) {
            autoAssignMandatoryCourseToAllEmployees(saved);
        }

        try {
            // notify creator
            String creatorEmail = fetchUserEmail(saved.getCreatedBy());
            if (creatorEmail != null) {
                String subject = "Course created: " + saved.getTitle();
                String message = "Your course has been created.\nTitle: " + saved.getTitle()
                        + "\nCategory: " + saved.getCategory()
                        + "\nCourseId: " + saved.getId();
                sendEmail(creatorEmail, subject, message);
            }

            // notify HR group : get HR users from user service
            List<String> hrEmails = fetchUsersEmailsByRole("HR");
            for (String hr : hrEmails) {
                sendEmail(hr, "New course created: " + saved.getTitle(),
                        "A new course was created by userId " + saved.getCreatedBy()
                                + "\nTitle: " + saved.getTitle());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return saved;
    }

    // ✅ IMPROVED METHOD with duplicate check and transaction management

    private void autoAssignMandatoryCourseToAllEmployees(Course course) {
        try {
            // Get all employees from User Service
            List<Map<String, Object>> allEmployees = fetchAllEmployees();
            System.out.println("👥 Found " + allEmployees.size() + " employees for mandatory course assignment");

            if (allEmployees.isEmpty()) {
                System.out.println("⚠️ No employees found for mandatory course assignment");
                return;
            }

            int enrolledCount = 0;
            int skippedCount = 0;

            for (Map<String, Object> employee : allEmployees) {
                try {
                    Long employeeId = ((Number) employee.get("id")).longValue();
                    String employeeEmail = (String) employee.get("email");
                    String employeeName = (String) employee.get("name");

                    // ✅ CHECK FOR EXISTING ENROLLMENT FIRST
                    Optional<Enrollment> existingEnrollment = enrollmentRepository
                            .findByCourseIdAndEmployeeId(course.getId(), employeeId);

                    if (existingEnrollment.isPresent()) {
                        System.out.println("⏭️ Employee " + employeeId + " already enrolled in course " + course.getId() + ", skipping");
                        skippedCount++;
                        continue;
                    }

                    // Create enrollment for each employee
                    Enrollment enrollment = Enrollment.builder()
                            .courseId(course.getId())
                            .employeeId(employeeId)
                            .progress(0)
                            .enrolledAt(LocalDateTime.now())
                            .status(Enrollment.Status.APPROVED) // Auto-approve mandatory courses
                            .build();

                    enrollmentRepository.save(enrollment);
                    enrolledCount++;

                    // Send notification
                    if (employeeEmail != null) {
                        sendMandatoryCourseEmail(employeeEmail, employeeName, course);
                    }

                } catch (Exception e) {
                    System.err.println("❌ Failed to enroll employee in mandatory course: " + e.getMessage());
                    // Continue with next employee instead of failing entire operation
                }
            }

            System.out.println("🎉 Mandatory course assignment completed: " +
                    enrolledCount + " enrolled, " + skippedCount + " skipped");

        } catch (Exception e) {
            System.err.println("❌ Failed to auto-assign mandatory course: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to assign mandatory course to employees", e);
        }
    }

    // ✅ IMPROVED EMAIL METHOD
    private void sendMandatoryCourseEmail(String employeeEmail, String employeeName, Course course) {
        try {
            String subject = "📚 New Mandatory Course Assigned: " + course.getTitle();
            String message = String.format(
                    "Hello %s,\n\n" +
                            "A new mandatory course has been assigned to you:\n\n" +
                            "📖 Course: %s\n" +
                            "📝 Description: %s\n" +
                            "⏱️ Duration: %d hours\n" +
                            "📈 Category: %s\n\n" +
                            "This course is mandatory and must be completed. " +
                            "Please access it from your 'My Courses' section and complete it as soon as possible.\n\n" +
                            "Best regards,\nTraining Team",
                    employeeName != null ? employeeName : "Employee",
                    course.getTitle(),
                    course.getDescription() != null ? course.getDescription() : "No description",
                    course.getDurationHours(),
                    course.getCategory() != null ? course.getCategory() : "General"
            );

            sendEmail(employeeEmail, subject, message);
            System.out.println("📧 Sent mandatory course notification to: " + employeeEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + employeeEmail + ": " + e.getMessage());
            // Don't throw - email failure shouldn't stop the enrollment process
        }
    }

    // ✅ IMPROVED FETCH EMPLOYEES METHOD WITH ERROR HANDLING
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAllEmployees() {
        try {
            String url = userServiceBaseUrl + "/api/users/role/EMPLOYEE";
            System.out.println("🔍 Fetching employees from: " + url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                System.err.println("❌ Null response from user service");
                return Collections.emptyList();
            }

            Boolean success = (Boolean) response.get("success");
            if (success == null || !success) {
                System.err.println("❌ User service returned error: " + response.get("message"));
                return Collections.emptyList();
            }

            List<Map<String, Object>> employees = (List<Map<String, Object>>) response.get("data");
            System.out.println("✅ Successfully fetched " + (employees != null ? employees.size() : 0) + " employees");
            return employees != null ? employees : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("❌ Failed to fetch employees from user service: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public Course updateCourse(Long id, CourseRequest request) {
        Course existing = getCourseById(id);
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setDurationHours(request.getDurationHours());
        existing.setPaid(request.isPaid());
        existing.setPrice(request.getPrice());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setMaterialIds(request.getMaterials()); // ✅ update material IDs
        existing.setMandatory(request.isMandatory()); // ✅ ADD THIS
        // 🆕 UNCOMMENT THIS - STATUS SHOULD BE UPDATED
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        return courseRepository.save(existing);
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        course.setStatus(Course.Status.INACTIVE);
        courseRepository.save(course);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + id));
    }

    @Override
    public List<Course> getAllActiveCourses() {
        return courseRepository.findByStatus(Course.Status.ACTIVE);
    }

    @Override
    public List<Course>getAllCourses(){
        return courseRepository.findAll();
    }

    @Override
    public Enrollment enrollCourse(EnrollmentRequest request) {
        Course course = getCourseById(request.getCourseId());
        enrollmentRepository.findByCourseIdAndEmployeeId(request.getCourseId(), request.getEmployeeId())
                .ifPresent(e -> { throw new BadRequestException("Already enrolled"); });

        Enrollment enrollment = Enrollment.builder()
                .courseId(course.getId())
                .employeeId(request.getEmployeeId())
                .progress(0)
                .enrolledAt(LocalDateTime.now())
                .status(course.isPaid() ? Enrollment.Status.PENDING_APPROVAL : Enrollment.Status.APPROVED)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Notifications:
        try {
            // employee email
            String employeeEmail = fetchUserEmail(saved.getEmployeeId());
            if (employeeEmail != null) {
                String subject = "Enrollment " + (saved.getStatus() == Enrollment.Status.APPROVED ? "confirmed" : "submitted");
                String message = "You have been enrolled in course: " + course.getTitle()
                        + "\nStatus: " + saved.getStatus();
                sendEmail(employeeEmail, subject, message);
            }

            // notify course creator
            String creatorEmail = fetchUserEmail(course.getCreatedBy());
            if (creatorEmail != null) {
                String subject = "Employee enrolled in your course: " + course.getTitle();
                String message = "EmployeeId " + saved.getEmployeeId() + " has enrolled in course " + course.getTitle()
                        + "\nEnrollmentId: " + saved.getId() + "\nStatus: " + saved.getStatus();
                sendEmail(creatorEmail, subject, message);
            }

            // if course requires approval (paid), notify HR group
            if (course.isPaid() && saved.getStatus() == Enrollment.Status.PENDING_APPROVAL) {
                List<String> hrEmails = fetchUsersEmailsByRole("HR");
                for (String hr : hrEmails) {
                    sendEmail(hr, "Enrollment requires approval for course: " + course.getTitle(),
                            "EnrollmentId " + saved.getId() + " for employeeId " + saved.getEmployeeId()
                                    + " needs approval.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return saved;
    }

    @Override
    public Enrollment approveEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollment.setStatus(Enrollment.Status.APPROVED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        // notify employee and course creator
        try {
            String employeeEmail = fetchUserEmail(saved.getEmployeeId());
            if (employeeEmail != null) {
                sendEmail(employeeEmail, "Enrollment approved", "Your enrollment has been approved for courseId " + saved.getCourseId());
            }
            Course course = getCourseById(saved.getCourseId());
            String creatorEmail = fetchUserEmail(course.getCreatedBy());
            if (creatorEmail != null) {
                sendEmail(creatorEmail, "Enrollment approved for your course", "EnrollmentId " + saved.getId() + " approved.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return saved;
    }

    @Override
    public Enrollment rejectEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollment.setStatus(Enrollment.Status.REJECTED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        try {
            String employeeEmail = fetchUserEmail(saved.getEmployeeId());
            if (employeeEmail != null) {
                sendEmail(employeeEmail, "Enrollment rejected", "Your enrollment has been rejected for courseId " + saved.getCourseId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return saved;
    }

    @Override
    public List<Enrollment> getEnrollmentsByEmployee(Long employeeId) {
        return enrollmentRepository.findByEmployeeId(employeeId);
    }

    // ---------- New methods ----------

    @Override
    public List<Course> getCoursesByCategory(String category) {
        return courseRepository.findByCategoryAndStatus(category, Course.Status.ACTIVE);
    }

    @Override
    public List<Course> searchCourses(String keyword) {
        return courseRepository.searchByKeyword(keyword);
    }

    @Override
    public List<Course> getCoursesCreatedBy(Long createdBy) {
        return courseRepository.findByCreatedBy(createdBy);
    }

    @Override
    public Enrollment updateProgress(Long enrollmentId, int progress) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (progress < 0 || progress > 100) {
            throw new BadRequestException("Progress must be between 0 and 100");
        }
        enrollment.setProgress(progress);
        Enrollment saved = enrollmentRepository.save(enrollment);

        // if progress is low, notify employee (threshold example: <50)
        try {
            if (progress < 50) {
                String employeeEmail = fetchUserEmail(saved.getEmployeeId());
                if (employeeEmail != null) {
                    sendEmail(employeeEmail, "Course progress reminder",
                            "Your progress for courseId " + saved.getCourseId() + " is " + progress + "%. Please continue.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return saved;
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Override
    public List<Map<String, Object>> getMaterialsForCourse(Long courseId) {
        Course course = getCourseById(courseId);
        List<Long> materialIds = course.getMaterialIds();
        if (materialIds == null || materialIds.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> materials = new ArrayList<>();

        for (Long id : materialIds) {
            try {
                Map<String, Object> resp = restTemplate.getForObject(
                        materialServiceBaseUrl + "/api/materials/" + id, Map.class);
                if (resp != null && Boolean.TRUE.equals(resp.get("success"))) {
                    Object data = resp.get("data");
                    if (data instanceof Map) {
                        Map<?, ?> mat = (Map<?, ?>) data;
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", mat.get("id"));
                        m.put("title", mat.get("title"));
                        m.put("type", mat.get("type"));
                        m.put("url", materialServiceBaseUrl + "/api/materials/download/" + m.get("id"));
                        materials.add(m);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return materials;
    }

    // Helpers: calls to User Service & Notification Service
    @SuppressWarnings("unchecked")
    private String fetchUserEmail(Long userId) {
        try {
            if (userId == null) return null;
            String url = userServiceBaseUrl + "/api/users/" + userId;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return null;
            Object data = resp.get("data");
            if (data instanceof Map) {
                Map<String,Object> userMap = (Map<String,Object>) data;
                Object emailObj = userMap.get("email");
                return emailObj == null ? null : emailObj.toString();
            }
        } catch (Exception ex) {
            // log and return null
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Query User Service to get list of users by role (e.g. "HR")
     * Calls {userServiceBaseUrl}/api/users/role/{role} which returns ApiResponse<List<UserResponse>>
     * Returns empty list if none.
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchUsersEmailsByRole(String role) {
        try {
            String url = userServiceBaseUrl + "/api/users/role/" + role;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return Collections.emptyList();
            Object data = resp.get("data");
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                List<String> emails = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map) {
                        Object email = ((Map)o).get("email");
                        if (email != null) emails.add(email.toString());
                    }
                }
                return emails;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Send single email via Notification Service (synchronous).
     * Notification Service expects JSON { "to":"...","subject":"...","message":"..." }.
     */
    private void sendEmail(String to, String subject, String message) {
        try {
            if (to == null || to.trim().isEmpty()) return;
            Map<String, String> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("message", message);
            restTemplate.postForObject(notificationServiceUrl, payload, String.class);
        } catch (Exception ex) {
            // log but do not throw (we don't want a notification failure to stop the operation)
            ex.printStackTrace();
        }
    }
    /* @Override
     public boolean checkExamEligibility(Long enrollmentId) {
         Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                 .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
         return enrollment.getProgress() == 100 && enrollment.getStatus() == Enrollment.Status.APPROVED;
     }*/
    @Override
    public boolean checkExamEligibility(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        // ✅ FIX: Allow both APPROVED and COMPLETED status
        boolean validStatus = enrollment.getStatus() == Enrollment.Status.APPROVED
                || enrollment.getStatus() == Enrollment.Status.COMPLETED;

        return enrollment.getProgress() == 100 && validStatus;
    }

    /* @Override
     public List<Map<String, Object>> getMyCourses(Long employeeId) {
         List<Enrollment> enrollments = enrollmentRepository.findByEmployeeId(employeeId);

         return enrollments.stream().map(enrollment -> {
             Course course = getCourseById(enrollment.getCourseId());
             Map<String, Object> map = new HashMap<>();
             map.put("course", course);
             map.put("progress", enrollment.getProgress());
             map.put("id", enrollment.getId()); // 🆕 ADD THIS - Enrollment ID
             map.put("status", enrollment.getStatus());
             return map;
         }).collect(Collectors.toList());
     }*/
    @Override
    public List<Map<String, Object>> getMyCourses(Long employeeId) {
        System.out.println("🎯 Getting courses for employee: " + employeeId);

        // 1. Get existing enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByEmployeeId(employeeId);
        System.out.println("📊 Found " + enrollments.size() + " enrollments");

        // 2. Get all ACTIVE courses and filter for mandatory ones
        List<Course> allActiveCourses = courseRepository.findByStatus(Course.Status.ACTIVE);
        System.out.println("📚 Found " + allActiveCourses.size() + " active courses in system");

        // 🆕 FIXED: Proper mandatory course filtering
        List<Course> mandatoryCourses = allActiveCourses.stream()
                .filter(course -> {
                    boolean isMandatory = course.isMandatory();
                    System.out.println("🔍 Course: " + course.getTitle() + " | isMandatory: " + isMandatory);
                    return isMandatory;
                })
                .collect(Collectors.toList());

        System.out.println("📗 Found " + mandatoryCourses.size() + " mandatory courses");

        // 3. Create a set of course IDs the employee is already enrolled in
        Set<Long> enrolledCourseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toSet());

        // 4. Combine enrolled courses + mandatory courses not yet enrolled
        List<Map<String, Object>> result = new ArrayList<>();

        // Add existing enrollments
        for (Enrollment enrollment : enrollments) {
            System.out.println("🔍 Processing enrollment ID: " + enrollment.getId() +
                    ", Course ID: " + enrollment.getCourseId() +
                    ", Status: " + enrollment.getStatus());

            Course course = getCourseById(enrollment.getCourseId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", enrollment.getId());
            map.put("course", course);
            map.put("progress", enrollment.getProgress());
            map.put("status", enrollment.getStatus());
            map.put("enrollmentType", "REGULAR");
            map.put("isMandatory", course.isMandatory()); // 🆕 ADD THIS
            result.add(map);
        }

        // 🆕 FIXED: Add mandatory courses that employee is not enrolled in yet
        int addedMandatory = 0;
        for (Course mandatoryCourse : mandatoryCourses) {
            if (!enrolledCourseIds.contains(mandatoryCourse.getId())) {
                System.out.println("➕ Adding mandatory course not enrolled: " + mandatoryCourse.getTitle());

                Map<String, Object> map = new HashMap<>();
                map.put("id", null); // No enrollment ID yet
                map.put("course", mandatoryCourse);
                map.put("progress", 0);
                map.put("status", Enrollment.Status.APPROVED); // Auto-approved for mandatory
                map.put("enrollmentType", "MANDATORY");
                map.put("isMandatory", true);
                result.add(map);
                addedMandatory++;
            }
        }

        System.out.println("✅ Returning " + result.size() + " courses to frontend ("
                + enrollments.size() + " enrolled + " + addedMandatory + " mandatory not enrolled)");

        return result;
    }
    @Override
    public List<Enrollment> getPendingEnrollments() {
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == Enrollment.Status.PENDING_APPROVAL)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> generateCourseReport() {
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        long completed = enrollmentRepository.findAll().stream()
                .filter(e -> e.getProgress() == 100 && e.getStatus() == Enrollment.Status.APPROVED)
                .count();
        long pending = enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == Enrollment.Status.PENDING_APPROVAL)
                .count();

        Map<String, Object> report = new HashMap<>();
        report.put  ("totalCourses", totalCourses);
        report.put("totalEnrollments", totalEnrollments);
        report.put("completedEnrollments", completed);
        report.put("pendingApprovals", pending);
        return report;
    }

    // 🆕 ADD PROGRESS INCREMENT MAPPING USING FUNCTION
    private final Function<String, Integer> progressIncrementMapper = contentType ->
            Optional.ofNullable(contentType)
                    .map(String::toUpperCase)
                    .map(type -> {
                        if ("VIDEO".equals(type) || "MODULE".equals(type)) return 25;
                        if ("PDF".equals(type) || "DOCUMENT".equals(type)) return 10;
                        if ("QUIZ".equals(type)) return 15;
                        return 10; // default
                    })
                    .orElse(10);

    // 🆕 ADD THIS METHOD USING STREAMS AND OPTIONAL
    @Override
    public Enrollment incrementProgress(Long enrollmentId, int incrementBy) {
        return Optional.of(enrollmentId)
                .map(enrollmentRepository::findById)
                .flatMap(Optional::ofNullable) // Handle Optional from repository
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"))
                .map(enrollment -> {
                    // Validate increment using Optional
                    Optional.of(incrementBy)
                            .filter(inc -> inc > 0 && inc <= 100)
                            .orElseThrow(() -> new BadRequestException("Progress increment must be between 1 and 100"));

                    // Calculate new progress
                    int newProgress = Math.min(enrollment.getProgress() + incrementBy, 100);
                    enrollment.setProgress(newProgress);

                    // Auto-update status using method reference
                    Optional.of(newProgress)
                            .filter(progress -> progress == 100)
                            .ifPresent(progress -> {
                                enrollment.setStatus(Enrollment.Status.COMPLETED);
                                sendCourseCompletionNotification(enrollment);
                            });

                    Optional.of(newProgress)
                            .filter(progress -> progress > 0)
                            .filter(progress -> enrollment.getStatus() == Enrollment.Status.APPROVED)
                            .ifPresent(progress -> enrollment.setStatus(Enrollment.Status.IN_PROGRESS));

                    return enrollmentRepository.save(enrollment);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
    }

    // 🆕 ADD THIS METHOD USING FUNCTION MAPPING
    @Override
    public Enrollment markContentComplete(Long enrollmentId, String contentType, String contentTitle) {
        return Optional.of(contentType)
                .map(progressIncrementMapper::apply)
                .map(incrementValue -> incrementProgress(enrollmentId, incrementValue))
                .orElseThrow(() -> new BadRequestException("Invalid content type"));
    }

    // 🆕 ADD THIS HELPER METHOD USING STREAMS
    private void sendCourseCompletionNotification(Enrollment enrollment) {
        Optional.of(enrollment)
                .map(Enrollment::getEmployeeId)
                .map(this::fetchUserEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .ifPresent(employeeEmail -> {
                    Course course = getCourseById(enrollment.getCourseId());
                    String subject = "🎉 Course Completed: " + course.getTitle();
                    String message = "Congratulations! You have successfully completed the course.\n\n" +
                            "Course: " + course.getTitle() + "\n" +
                            "Completed on: " + java.time.LocalDate.now() + "\n" +
                            "You can now take the final exam to get certified!";
                    sendEmail(employeeEmail, subject, message);

                    // Notify managers using streams
                    fetchUsersEmailsByRole("MANAGER").stream()
                            .filter(managerEmail -> managerEmail != null && !managerEmail.trim().isEmpty())
                            .forEach(managerEmail ->
                                    sendEmail(managerEmail,
                                            "Employee Course Completion: " + course.getTitle(),
                                            "Employee ID: " + enrollment.getEmployeeId() +
                                                    " has completed course: " + course.getTitle()
                                    )
                            );
                });
    }
    @Override
    public List<Course> searchCoursesByName(String name) {
        return courseRepository.findByTitleContainingIgnoreCase(name);
    }

    // 🆕 AUTOMATIC PROGRESS REMINDER SYSTEM - Add these methods

    /**
     * AUTOMATIC METHOD: Checks and sends reminders daily without admin action
     * This runs automatically every day at 9:00 AM
     */
    @Override
    @Scheduled(cron = "0 0 9 * * ?") // Runs daily at 9:00 AM
    public void checkAndSendProgressReminders() {
        try {
            System.out.println("🔄 [AUTOMATIC] Starting daily progress reminders check...");

            List<Enrollment> allEnrollments = enrollmentRepository.findAll();
            LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);

            int remindersSent = 0;
            int eligibleEnrollments = 0;

            for (Enrollment enrollment : allEnrollments) {
                // Check if enrollment meets criteria for reminder
                if (shouldSendReminder(enrollment, fifteenDaysAgo)) {
                    eligibleEnrollments++;
                    try {
                        sendAutomaticReminderForEnrollment(enrollment.getId());
                        remindersSent++;
                        System.out.println("✅ [AUTOMATIC] Sent reminder for enrollment ID: " + enrollment.getId());

                        // Small delay to avoid overwhelming email service
                        Thread.sleep(50);

                    } catch (Exception e) {
                        System.err.println("❌ [AUTOMATIC] Failed to send reminder for enrollment " + enrollment.getId() + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("🎉 [AUTOMATIC] Daily reminders completed: " +
                    remindersSent + " sent out of " + eligibleEnrollments + " eligible enrollments");

        } catch (Exception e) {
            System.err.println("💥 [AUTOMATIC] Error in automatic progress reminders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Map<String, Object>> getEnrollmentsNeedingReminders() {
        try {
            List<Enrollment> allEnrollments = enrollmentRepository.findAll();
            LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);

            List<Map<String, Object>> result = new ArrayList<>();

            for (Enrollment enrollment : allEnrollments) {
                if (shouldSendReminder(enrollment, fifteenDaysAgo)) {
                    // Calculate days since enrollment
                    long daysSinceEnrollment = java.time.temporal.ChronoUnit.DAYS.between(
                            enrollment.getEnrolledAt().toLocalDate(),
                            java.time.LocalDate.now()
                    );

                    // Get employee and course details
                    Map<String, Object> employeeInfo = getEmployeeInfoForReminder(enrollment.getEmployeeId());
                    Course course = getCourseById(enrollment.getCourseId());

                    Map<String, Object> enrollmentData = new HashMap<>();
                    enrollmentData.put("enrollmentId", enrollment.getId());
                    enrollmentData.put("employeeId", enrollment.getEmployeeId());
                    enrollmentData.put("employeeName", employeeInfo.get("employeeName"));
                    enrollmentData.put("employeeEmail", employeeInfo.get("employeeEmail"));
                    enrollmentData.put("courseId", enrollment.getCourseId());
                    enrollmentData.put("courseName", course.getTitle());
                    enrollmentData.put("enrolledAt", enrollment.getEnrolledAt());
                    enrollmentData.put("daysSinceEnrollment", daysSinceEnrollment);
                    enrollmentData.put("progress", enrollment.getProgress());
                    enrollmentData.put("status", enrollment.getStatus().toString());

                    result.add(enrollmentData);
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("❌ Error getting enrollments needing reminders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void sendAutomaticReminderForEnrollment(Long enrollmentId) {
        try {
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

            // Get employee and course details
            Map<String, Object> employeeInfo = getEmployeeInfoForReminder(enrollment.getEmployeeId());
            Course course = getCourseById(enrollment.getCourseId());

            // Calculate days since enrollment
            long daysSinceEnrollment = java.time.temporal.ChronoUnit.DAYS.between(
                    enrollment.getEnrolledAt().toLocalDate(),
                    java.time.LocalDate.now()
            );

            // Send email to employee
            sendAutomaticReminderEmail(employeeInfo, course, enrollment, daysSinceEnrollment);

            // Also notify manager
            sendManagerNotification(employeeInfo, course, enrollment, daysSinceEnrollment);

        } catch (Exception e) {
            System.err.println("❌ [AUTOMATIC] Error sending automatic reminder: " + e.getMessage());
            throw new RuntimeException("Failed to send automatic reminder", e);
        }
    }

    /**
     * Helper method to check if reminder should be sent for an enrollment
     */
    private boolean shouldSendReminder(Enrollment enrollment, LocalDateTime fifteenDaysAgo) {
        return enrollment.getEnrolledAt() != null &&
                enrollment.getEnrolledAt().isBefore(fifteenDaysAgo) &&
                enrollment.getProgress() < 50 &&
                (enrollment.getStatus() == Enrollment.Status.APPROVED ||
                        enrollment.getStatus() == Enrollment.Status.IN_PROGRESS);
    }

    /**
     * Send automatic reminder email to employee
     */
    private void sendAutomaticReminderEmail(Map<String, Object> employeeInfo, Course course,
                                            Enrollment enrollment, long daysSinceEnrollment) {
        try {
            String employeeEmail = (String) employeeInfo.get("employeeEmail");
            String employeeName = (String) employeeInfo.get("employeeName");

            if (employeeEmail != null && !employeeEmail.trim().isEmpty()) {
                String subject = "📚 Course Progress Reminder - " + course.getTitle();
                String message = buildAutomaticReminderMessage(employeeName, course, enrollment, daysSinceEnrollment);

                sendEmail(employeeEmail, subject, message);

                System.out.println("📧 [AUTOMATIC] Email sent to: " + employeeEmail);
            }
        } catch (Exception e) {
            System.err.println("❌ [AUTOMATIC] Error sending employee email: " + e.getMessage());
        }
    }

    /**
     * Build the automatic reminder email message
     */
    private String buildAutomaticReminderMessage(String employeeName, Course course,
                                                 Enrollment enrollment, long daysSinceEnrollment) {
        StringBuilder message = new StringBuilder();

        message.append("Dear ").append(employeeName != null ? employeeName : "Employee").append(",\n\n");

        message.append("This is an automatic reminder from CSLLP Learning Platform.\n\n");

        message.append("We noticed that you enrolled in the course \"").append(course.getTitle()).append("\" ");
        message.append("on ").append(enrollment.getEnrolledAt().toLocalDate()).append(" ");
        message.append("(").append(daysSinceEnrollment).append(" days ago), ");
        message.append("but your progress is currently at ").append(enrollment.getProgress()).append("%.\n\n");

        if (enrollment.getProgress() == 0) {
            message.append("🔸 You haven't started this course yet.\n");
            message.append("🔸 Starting early helps in better learning and completion.\n\n");
        } else {
            message.append("🔸 Your progress is below 50%.\n");
            message.append("🔸 Continuing regularly will help you complete the course successfully.\n\n");
        }

        message.append("**Action Required:**\n");
        message.append("Please access the course from your 'My Courses' section and continue your learning.\n\n");

        message.append("**Course Details:**\n");
        message.append("• Course: ").append(course.getTitle()).append("\n");
        message.append("• Enrolled: ").append(enrollment.getEnrolledAt().toLocalDate()).append("\n");
        message.append("• Current Progress: ").append(enrollment.getProgress()).append("%\n");
        message.append("• Days Since Enrollment: ").append(daysSinceEnrollment).append("\n\n");

        message.append("**Need Help?**\n");
        message.append("• Contact your manager for guidance\n");
        message.append("• Reach out to training support team\n");
        message.append("• Check course materials and resources\n\n");

        message.append("Best regards,\n");
        message.append("CSLLP Learning Platform\n");
        message.append("Automated Progress Monitoring System");

        return message.toString();
    }

    /**
     * Send notification to employee's manager
     */
    private void sendManagerNotification(Map<String, Object> employeeInfo, Course course,
                                         Enrollment enrollment, long daysSinceEnrollment) {
        try {
            Long managerId = (Long) employeeInfo.get("managerId");
            if (managerId != null) {
                String managerEmail = fetchUserEmail(managerId);
                String employeeName = (String) employeeInfo.get("employeeName");

                if (managerEmail != null && !managerEmail.trim().isEmpty()) {
                    String subject = "👥 Employee Course Progress Alert - " + employeeName;
                    String message = buildManagerNotificationMessage(employeeName, course, enrollment, daysSinceEnrollment);

                    sendEmail(managerEmail, subject, message);

                    System.out.println("📧 [AUTOMATIC] Manager notification sent to: " + managerEmail);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [AUTOMATIC] Error sending manager notification: " + e.getMessage());
            // Don't throw - manager notification is optional
        }
    }

    /**
     * Build manager notification message
     */
    private String buildManagerNotificationMessage(String employeeName, Course course,
                                                   Enrollment enrollment, long daysSinceEnrollment) {
        StringBuilder message = new StringBuilder();

        message.append("Dear Manager,\n\n");

        message.append("Automatic Progress Alert for Your Team Member:\n\n");

        message.append("**Employee:** ").append(employeeName).append("\n");
        message.append("**Course:** ").append(course.getTitle()).append("\n");
        message.append("**Enrollment Date:** ").append(enrollment.getEnrolledAt().toLocalDate()).append("\n");
        message.append("**Days Since Enrollment:** ").append(daysSinceEnrollment).append("\n");
        message.append("**Current Progress:** ").append(enrollment.getProgress()).append("%\n\n");

        if (enrollment.getProgress() == 0) {
            message.append("📊 Status: Course not started yet\n");
        } else {
            message.append("📊 Status: Progress below 50%\n");
        }

        message.append("\n**Automatic Action Taken:**\n");
        message.append("• Reminder email sent to employee\n");
        message.append("• Encouragement to continue course progress\n\n");

        message.append("**Suggested Manager Actions:**\n");
        message.append("• Check in with the employee about any challenges\n");
        message.append("• Provide additional support or resources if needed\n");
        message.append("• Discuss learning priorities in your next meeting\n\n");

        message.append("This is an automated notification from CSLLP Learning Platform.\n\n");

        message.append("Best regards,\n");
        message.append("Learning & Development Team");

        return message.toString();
    }

    /**
     * Enhanced employee info fetcher for reminders
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getEmployeeInfoForReminder(Long employeeId) {
        try {
            String url = userServiceBaseUrl + "/api/users/" + employeeId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("data") instanceof Map) {
                Map<String, Object> employeeData = (Map<String, Object>) response.get("data");

                // Extract name
                String firstName = (String) employeeData.get("firstName");
                String lastName = (String) employeeData.get("lastName");
                String employeeName = (firstName != null ? firstName : "") +
                        (lastName != null ? " " + lastName : "");
                if (employeeName.trim().isEmpty()) {
                    employeeName = "Employee " + employeeId;
                }

                // Extract other details
                String email = (String) employeeData.get("email");
                Object managerId = employeeData.get("managerId");

                Map<String, Object> info = new HashMap<>();
                info.put("employeeName", employeeName.trim());
                info.put("employeeEmail", email != null ? email : "unknown@example.com");
                info.put("managerId", managerId);

                return info;
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching employee info for ID " + employeeId + ": " + e.getMessage());
        }

        // Return default values
        Map<String, Object> defaultInfo = new HashMap<>();
        defaultInfo.put("employeeName", "Employee " + employeeId);
        defaultInfo.put("employeeEmail", "unknown@example.com");
        defaultInfo.put("managerId", null);

        return defaultInfo;
    }
}