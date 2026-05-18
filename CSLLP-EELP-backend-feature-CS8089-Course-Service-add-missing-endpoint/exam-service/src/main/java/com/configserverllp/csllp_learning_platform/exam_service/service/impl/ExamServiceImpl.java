package com.configserverllp.csllp_learning_platform.exam_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.configserverllp.csllp_learning_platform.exam_service.dto.*;
import com.configserverllp.csllp_learning_platform.exam_service.entity.*;
import com.configserverllp.csllp_learning_platform.exam_service.exception.BadRequestException;
import com.configserverllp.csllp_learning_platform.exam_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.exam_service.repository.*;
import com.configserverllp.csllp_learning_platform.exam_service.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Value("${course.service.url}")
    private String courseServiceUrl;

    @Value("${analytics.service.url}")
    private String analyticsServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =================== EXAM CRUD ===================

    @Override
    public Exam createExam(ExamRequest req, Long creatorId) {
        String creatorRole = fetchUserRole(creatorId);

        if (!"ADMIN".equalsIgnoreCase(creatorRole) && !"MANAGER".equalsIgnoreCase(creatorRole)) {
            throw new BadRequestException("Only ADMIN or MANAGER can create exams");
        }

        Exam exam = Exam.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .durationMinutes(req.getDurationMinutes())
                .createdBy(creatorId)
                .courseId(req.getCourseId())
                .examType("MCQ")
                .status(Exam.Status.ACTIVE)
                .build();

        Exam saved = examRepository.save(exam);

        if ("MANAGER".equalsIgnoreCase(creatorRole)) {
            notifyAdminsAboutManagerExam(creatorId, saved);
        }

        return saved;
    }

    @Override
    public Exam updateExam(Long id, ExamRequest req) {
        Exam existing = getExamById(id);
        existing.setTitle(req.getTitle());
        existing.setDescription(req.getDescription());
        existing.setDurationMinutes(req.getDurationMinutes());
        return examRepository.save(existing);
    }

    @Override
    public void deleteExam(Long id) {
        Exam e = getExamById(id);
        e.setStatus(Exam.Status.INACTIVE);
        examRepository.save(e);
    }

    @Override
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    @Override
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    @Override
    public Exam getExamByCourse(Long courseId) {
        return examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam for course not found"));
    }

    // =================== QUESTIONS ===================

    @Override
    public Question addQuestion(Long examId, QuestionRequest req) {
        Exam e = getExamById(examId);

        if (!"MCQ".equalsIgnoreCase(req.getQuestionType())) {
            throw new BadRequestException("Only MCQ questions are allowed");
        }

        if (req.getOptions() == null || req.getOptions().size() < 2) {
            throw new BadRequestException("MCQ must have at least 2 options");
        }

        Question q = Question.builder()
                .examId(examId)
                .questionText(req.getQuestionText())
                .questionType("MCQ")
                .marks(req.getMarks())
                .build();

        List<Option> opts = new ArrayList<>();
        for (OptionRequest or : req.getOptions()) {
            Option opt = Option.builder()
                    .question(q)
                    .text(or.getText())
                    .isCorrect(or.isCorrect())
                    .build();
            opts.add(opt);
        }
        q.setOptions(opts);

        return questionRepository.save(q);
    }

    @Override
    public List<Question> getQuestionsByExam(Long examId) {
        getExamById(examId);
        return questionRepository.findByExamId(examId);
    }

    // =================== ATTEMPTS / RESULTS ===================

    @Override
    public ExamAttempt startAttempt(Long examId, Long employeeId) {
        Exam exam = getExamById(examId);

        // Check eligibility
        ExamEligibilityDTO eligibility = checkExamEligibility(examId, employeeId);
        if (!eligibility.getIsEligible()) {
            throw new BadRequestException(eligibility.getMessage());
        }

        // ✅ FIXED: Check for existing in-progress attempt and return it instead of throwing error
        Optional<ExamAttempt> existingAttempt = attemptRepository.findByExamIdAndEmployeeId(examId, employeeId);
        if (existingAttempt.isPresent() && "IN_PROGRESS".equals(existingAttempt.get().getStatus())) {
            // Return the existing in-progress attempt instead of throwing an error
            return existingAttempt.get();
        }

        // If no existing in-progress attempt, create a new one
        ExamAttempt attempt = ExamAttempt.builder()
                .examId(examId)
                .employeeId(employeeId)
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .build();

        return attemptRepository.save(attempt);
    }

    @Override
    public ExamAttempt submitAttempt(Long examId, SubmitAttemptRequest req) {
        getExamById(examId);
        ExamAttempt attempt = attemptRepository.findByExamIdAndEmployeeId(examId, req.getEmployeeId())
                .orElseThrow(() -> new BadRequestException("Attempt not started"));

        try {
            String json = objectMapper.writeValueAsString(req.getAnswers());
            attempt.setAnswersJson(json);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid answers payload");
        }

        double obtained = 0.0;
        int correctCount = 0;
        int wrongCount = 0;

        List<Question> questions = questionRepository.findByExamId(examId);
        Map<String, Object> answers = req.getAnswers();

        for (Question q : questions) {
            Object provided = answers.get(String.valueOf(q.getId()));
            if (provided instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> selectedOptionIds = (List<Integer>) provided;
                Set<Long> correctIds = q.getOptions().stream()
                        .filter(Option::isCorrect)
                        .map(Option::getId)
                        .collect(Collectors.toSet());
                Set<Long> selected = selectedOptionIds.stream().map(Integer::longValue).collect(Collectors.toSet());

                if (correctIds.equals(selected)) {
                    obtained += Optional.ofNullable(q.getMarks()).orElse(0);
                    correctCount++;
                } else {
                    wrongCount++;
                }
            }
        }

        attempt.setScore(obtained);
        attempt.setStatus("COMPLETED");
        attempt.setSubmittedAt(LocalDateTime.now());

        // Calculate time taken
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            long minutes = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMinutes();
            attempt.setTimeTakenMinutes((int) minutes);
        }

        ExamAttempt savedAttempt = attemptRepository.save(attempt);

        // Optionally, you can store correct/wrong count in attempt if needed
        // Or just calculate on the fly in enrichExamResults()

        sendAnalyticsReport(savedAttempt);

        return savedAttempt;
    }

    @Override
    public ExamAttempt getAttempt(Long examId, Long employeeId) {
        return attemptRepository.findByExamIdAndEmployeeId(examId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found"));
    }

    // =================== RESULTS & REPORTING ===================

    @Override
    public List<ExamResultDTO> getAllExamResults() {
        List<ExamAttempt> attempts = attemptRepository.findAll();
        return enrichExamResults(attempts);
    }

    @Override
    public List<ExamResultDTO> getExamResultsByExam(Long examId) {
        getExamById(examId);
        List<ExamAttempt> attempts = attemptRepository.findByExamId(examId);
        return enrichExamResults(attempts);
    }

    @Override
    public List<EmployeeResultDTO> getEmployeeResults(Long employeeId) {
        if (employeeId == null) throw new BadRequestException("Employee ID is required");

        List<ExamAttempt> attempts = attemptRepository.findByEmployeeId(employeeId);
        if (attempts.isEmpty()) {
            throw new ResourceNotFoundException("No exam results found for this employee");
        }

        return attempts.stream().map(attempt -> {
            Exam exam = getExamById(attempt.getExamId());
            Map<String, Object> courseInfo = getCourseInfo(exam.getCourseId());
            Map<String, Object> employeeInfo = getEmployeeInfo(employeeId);

            // Calculate total marks for the exam
            List<Question> questions = questionRepository.findByExamId(exam.getId());
            Integer totalMarks = questions.stream().mapToInt(Question::getMarks).sum();

            // Calculate percentage
            Double percentage = totalMarks > 0 ? (attempt.getScore() / totalMarks) * 100 : 0.0;

            // Calculate grade
            String grade = calculateGrade(percentage);

            return EmployeeResultDTO.builder()
                    .attemptId(attempt.getId())
                    .examId(exam.getId())
                    .examTitle(exam.getTitle())
                    .courseId(exam.getCourseId())
                    .courseName((String) courseInfo.get("courseName"))
                    .score(attempt.getScore())
                    .totalMarks(totalMarks)
                    .percentage(percentage)
                    .status(attempt.getStatus())
                    .startedAt(attempt.getStartedAt())
                    .submittedAt(attempt.getSubmittedAt())
                    .feedback(attempt.getFeedback())
                    .grade(grade)
                    .timeTakenMinutes(calculateTimeTakenMinutes(attempt))
                    .build();
        }).collect(Collectors.toList());
    }

    // =================== EMPLOYEE SPECIFIC METHODS ===================

    @Override
    public List<EmployeeExamDTO> getExamsForEmployee(Long employeeId) {
        // Get employee's enrolled courses
        List<Map<String, Object>> enrollments = getEmployeeEnrollments(employeeId);
        Set<Long> enrolledCourseIds = enrollments.stream()
                .map(enrollment -> ((Number) enrollment.get("courseId")).longValue())
                .collect(Collectors.toSet());

        List<Exam> allExams = examRepository.findAll();
        List<EmployeeExamDTO> result = new ArrayList<>();

        for (Exam exam : allExams) {
            // Check if employee is enrolled in the course
            if (!enrolledCourseIds.contains(exam.getCourseId())) {
                continue; // Skip exams for courses the employee is not enrolled in
            }

            // Get course progress - FIXED: Use the corrected method
            Integer courseProgress = getCourseProgress(employeeId, exam.getCourseId());

            // Check eligibility - KEEP 100% requirement
            boolean isEligible = courseProgress >= 100;
            String status = isEligible ? "AVAILABLE" : "LOCKED";
            String eligibilityMessage = isEligible ?
                    "You can take this exam anytime" :
                    "You cannot take this exam until the course is completed. Current progress: " + courseProgress + "%";

            // Check existing attempt
            Optional<ExamAttempt> attemptOpt = attemptRepository.findByExamIdAndEmployeeId(exam.getId(), employeeId);
            String attemptStatus = attemptOpt.map(ExamAttempt::getStatus).orElse("NOT_STARTED");

            Map<String, Object> courseInfo = getCourseInfo(exam.getCourseId());

            result.add(EmployeeExamDTO.builder()
                    .examId(exam.getId())
                    .title(exam.getTitle())
                    .description(exam.getDescription())
                    .durationMinutes(exam.getDurationMinutes())
                    .courseId(exam.getCourseId())
                    .courseName((String) courseInfo.get("courseName"))
                    .status(status)
                    .attemptStatus(attemptStatus)
                    .courseProgress(courseProgress)
                    .isEligible(isEligible)
                    .eligibilityMessage(eligibilityMessage)
                    .build());
        }

        return result;
    }

    @Override
    public ExamEligibilityDTO checkExamEligibility(Long examId, Long employeeId) {
        Exam exam = getExamById(examId);

        // 🆕 ADD: Debug logging
        System.out.println("🔍 Checking eligibility for exam: " + examId + ", employee: " + employeeId + ", course: " + exam.getCourseId());

        // Check if employee is enrolled in the course
        List<Map<String, Object>> enrollments = getEmployeeEnrollments(employeeId);
        boolean isEnrolled = enrollments.stream()
                .anyMatch(enrollment -> ((Number) enrollment.get("courseId")).longValue() == exam.getCourseId());

        if (!isEnrolled) {
            return ExamEligibilityDTO.builder()
                    .isEligible(false)
                    .message("You are not enrolled in this course")
                    .courseProgress(0)
                    .requiredProgress(100)
                    .hasExistingAttempt(false)
                    .build();
        }

        // Check course progress - FIXED: Use the corrected method
        Integer courseProgress = getCourseProgress(employeeId, exam.getCourseId());

        // 🆕 ADD: Debug logging
        System.out.println("🔍 Course progress for employee " + employeeId + " in course " + exam.getCourseId() + ": " + courseProgress + "%");

        // Check eligibility - KEEP 100% requirement
        boolean isEligible = courseProgress >= 100;

        // Check existing attempts
        Optional<ExamAttempt> existingAttempt = attemptRepository.findByExamIdAndEmployeeId(examId, employeeId);
        boolean hasExistingAttempt = existingAttempt.isPresent();
        String existingAttemptStatus = existingAttempt.map(ExamAttempt::getStatus).orElse(null);

        String message;
        if (!isEligible) {
            message = "You cannot take this exam until the course is completed. Current progress: " + courseProgress + "%";
        } else if (hasExistingAttempt && "COMPLETED".equals(existingAttemptStatus)) {
            message = "You have already completed this exam";
            isEligible = false; // Prevent re-taking completed exams
        } else if (hasExistingAttempt && "IN_PROGRESS".equals(existingAttemptStatus)) {
            message = "You have an attempt in progress";
            isEligible = true; // Allow continuing in-progress attempts
        } else {
            message = "You can take this exam anytime";
        }

        return ExamEligibilityDTO.builder()
                .isEligible(isEligible)
                .message(message)
                .courseProgress(courseProgress)
                .requiredProgress(100)
                .hasExistingAttempt(hasExistingAttempt)
                .existingAttemptStatus(existingAttemptStatus)
                .build();
    }

    // =================== ADMIN/MANAGER REPORTS ===================

    @Override
    public List<EmployeePerformanceReportDTO> getEmployeePerformanceReport(Long employeeId, Long courseId) {
        List<ExamAttempt> attempts;

        if (employeeId != null && courseId != null) {
            // Filter by both employee and course
            attempts = attemptRepository.findByEmployeeId(employeeId).stream()
                    .filter(attempt -> {
                        Exam exam = getExamById(attempt.getExamId());
                        return exam.getCourseId().equals(courseId);
                    })
                    .collect(Collectors.toList());
        } else if (employeeId != null) {
            // Filter by employee only
            attempts = attemptRepository.findByEmployeeId(employeeId);
        } else if (courseId != null) {
            // Filter by course only
            List<Exam> courseExams = examRepository.findAll().stream()
                    .filter(exam -> exam.getCourseId().equals(courseId))
                    .collect(Collectors.toList());
            attempts = new ArrayList<>();
            for (Exam exam : courseExams) {
                attempts.addAll(attemptRepository.findByExamId(exam.getId()));
            }
        } else {
            // All attempts
            attempts = attemptRepository.findAll();
        }

        // Group by employee and course
        Map<Long, Map<Long, List<ExamAttempt>>> employeeCourseAttempts = new HashMap<>();

        for (ExamAttempt attempt : attempts) {
            Exam exam = getExamById(attempt.getExamId());
            Long currentEmployeeId = attempt.getEmployeeId();
            Long currentCourseId = exam.getCourseId();

            employeeCourseAttempts
                    .computeIfAbsent(currentEmployeeId, k -> new HashMap<>())
                    .computeIfAbsent(currentCourseId, k -> new ArrayList<>())
                    .add(attempt);
        }

        List<EmployeePerformanceReportDTO> report = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, List<ExamAttempt>>> employeeEntry : employeeCourseAttempts.entrySet()) {
            Long currentEmployeeId = employeeEntry.getKey();
            Map<String, Object> employeeInfo = getEmployeeInfo(currentEmployeeId);

            for (Map.Entry<Long, List<ExamAttempt>> courseEntry : employeeEntry.getValue().entrySet()) {
                Long currentCourseId = courseEntry.getKey();
                List<ExamAttempt> courseAttempts = courseEntry.getValue();
                Map<String, Object> courseInfo = getCourseInfo(currentCourseId);

                Double averageScore = courseAttempts.stream()
                        .mapToDouble(attempt -> Optional.ofNullable(attempt.getScore()).orElse(0.0))
                        .average()
                        .orElse(0.0);

                Double bestScore = courseAttempts.stream()
                        .mapToDouble(attempt -> Optional.ofNullable(attempt.getScore()).orElse(0.0))
                        .max()
                        .orElse(0.0);

                Double worstScore = courseAttempts.stream()
                        .mapToDouble(attempt -> Optional.ofNullable(attempt.getScore()).orElse(0.0))
                        .min()
                        .orElse(0.0);

                LocalDateTime lastAttemptDate = courseAttempts.stream()
                        .map(ExamAttempt::getSubmittedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                String performanceLevel = calculatePerformanceLevel(averageScore);

                report.add(EmployeePerformanceReportDTO.builder()
                        .employeeId(currentEmployeeId)
                        .employeeName((String) employeeInfo.get("employeeName"))
                        .employeeEmail((String) employeeInfo.get("employeeEmail"))
                        .department((String) employeeInfo.get("department"))
                        .courseId(currentCourseId)
                        .courseName((String) courseInfo.get("courseName"))
                        .totalExamsAttempted((int) courseAttempts.stream().map(ExamAttempt::getExamId).distinct().count())
                        .averageScore(averageScore)
                        .bestScore(bestScore)
                        .worstScore(worstScore)
                        .totalAttempts(courseAttempts.size())
                        .performanceLevel(performanceLevel)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
            }
        }

        return report;
    }

    @Override
    public List<CourseEffectivenessReportDTO> getCourseEffectivenessReport() {
        List<Exam> allExams = examRepository.findAll();
        Map<Long, List<Exam>> examsByCourse = allExams.stream()
                .collect(Collectors.groupingBy(Exam::getCourseId));

        List<CourseEffectivenessReportDTO> report = new ArrayList<>();

        for (Map.Entry<Long, List<Exam>> entry : examsByCourse.entrySet()) {
            Long courseId = entry.getKey();
            List<Exam> courseExams = entry.getValue();
            Map<String, Object> courseInfo = getCourseInfo(courseId);

            // Get all attempts for all exams in this course
            List<ExamAttempt> allCourseAttempts = new ArrayList<>();
            for (Exam exam : courseExams) {
                allCourseAttempts.addAll(attemptRepository.findByExamId(exam.getId()));
            }

            // Calculate metrics
            int totalEnrollments = getCourseEnrollmentsCount(courseId);
            int totalExamAttempts = allCourseAttempts.size();

            double averageScore = allCourseAttempts.stream()
                    .mapToDouble(attempt -> Optional.ofNullable(attempt.getScore()).orElse(0.0))
                    .average()
                    .orElse(0.0);

            double passRate = allCourseAttempts.stream()
                    .filter(attempt -> {
                        Exam exam = getExamById(attempt.getExamId());
                        List<Question> questions = questionRepository.findByExamId(exam.getId());
                        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
                        double passThreshold = totalMarks * 0.6; // 60% pass threshold
                        return attempt.getScore() != null && attempt.getScore() >= passThreshold;
                    })
                    .count() / (double) Math.max(totalExamAttempts, 1);

            double completionRate = totalEnrollments > 0 ?
                    (double) totalExamAttempts / totalEnrollments : 0.0;

            int totalQuestions = courseExams.stream()
                    .mapToInt(exam -> questionRepository.findByExamId(exam.getId()).size())
                    .sum();

            String difficultyLevel = calculateDifficultyLevel(averageScore);
            String effectivenessRating = calculateEffectivenessRating(passRate, completionRate);

            report.add(CourseEffectivenessReportDTO.builder()
                    .courseId(courseId)
                    .courseName((String) courseInfo.get("courseName"))
                    .totalEnrollments(totalEnrollments)
                    .totalExamAttempts(totalExamAttempts)
                    .averageScore(averageScore)
                    .passRate(passRate * 100)
                    .completionRate(completionRate * 100)
                    .totalQuestions(totalQuestions)
                    .difficultyLevel(difficultyLevel)
                    .effectivenessRating(effectivenessRating)
                    .build());
        }

        return report;
    }

    // =================== ANALYTICS ===================

    @Override
    public List<AnalyticsReportDto> getAnalytics(Long courseId, Long employeeId, String fromDate, String toDate) {
        try {
            // Build safe URL
            StringBuilder urlBuilder = new StringBuilder(analyticsServiceUrl + "/api/analytics");
            boolean hasQuery = false;

            if (courseId != null) {
                urlBuilder.append(hasQuery ? "&" : "?").append("courseId=").append(courseId);
                hasQuery = true;
            }
            if (employeeId != null) {
                urlBuilder.append(hasQuery ? "&" : "?").append("employeeId=").append(employeeId);
                hasQuery = true;
            }
            if (fromDate != null) {
                urlBuilder.append(hasQuery ? "&" : "?").append("fromDate=").append(fromDate);
                hasQuery = true;
            }
            if (toDate != null) {
                urlBuilder.append(hasQuery ? "&" : "?").append("toDate=").append(toDate);
            }

            String url = urlBuilder.toString();
            System.out.println("🔗 Fetching analytics from: " + url);

            Object response = restTemplate.getForObject(url, Object.class);
            if (response instanceof Map map && map.get("data") instanceof List<?> list) {
                // ✅ Analytics service returned wrapped data: { data: [...] }
                ObjectMapper mapper = new ObjectMapper();
                return list.stream()
                        .map(item -> mapper.convertValue(item, AnalyticsReportDto.class))
                        .toList();
            } else if (response instanceof List<?> list) {
                // ✅ Direct list return
                ObjectMapper mapper = new ObjectMapper();
                return list.stream()
                        .map(item -> mapper.convertValue(item, AnalyticsReportDto.class))
                        .toList();
            } else if (response instanceof AnalyticsReportDto[] arr) {
                return Arrays.asList(arr);
            } else {
                System.err.println("⚠️ Unexpected analytics response format: " + response);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch analytics data. Please try again later.");
        }
    }

    // =================== NEW METHODS FOR REPORTING ===================

    @Override
    public Long getPendingExamsCount(Long employeeId) {
        try {
            List<Map<String, Object>> enrollments = getEmployeeEnrollments(employeeId);

            long pendingCount = 0;
            for (Map<String, Object> enrollment : enrollments) {
                Long courseId = ((Number) enrollment.get("courseId")).longValue();
                Integer progress = (Integer) enrollment.get("progress");

                // If course has exam and progress < 100%, count as pending
                if (progress < 100 && examRepository.findByCourseId(courseId).isPresent()) {
                    pendingCount++;
                }
            }
            return pendingCount;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @Override
    public Long getTotalEmployeesCount() {
        try {
            String countUrl = userServiceUrl + "/api/users/count/employees";
            Map<?, ?> countResp = restTemplate.getForObject(countUrl, Map.class);
            return countResp != null && countResp.get("data") != null ?
                    ((Number) countResp.get("data")).longValue() : 0L;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    @Override
    public List<ExamResultDTO> getManagerTeamResults(Long managerId) {
        try {
            // Get manager's team employees
            List<Map<String, Object>> teamEmployees = getManagerTeamEmployees(managerId);
            List<Long> teamEmployeeIds = teamEmployees.stream()
                    .map(emp -> ((Number) emp.get("id")).longValue())
                    .collect(Collectors.toList());

            // Get attempts for team employees
            List<ExamAttempt> teamAttempts = attemptRepository.findAll().stream()
                    .filter(attempt -> teamEmployeeIds.contains(attempt.getEmployeeId()))
                    .collect(Collectors.toList());

            return enrichExamResults(teamAttempts);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // =================== HELPER METHODS ===================

    private List<ExamResultDTO> enrichExamResults(List<ExamAttempt> attempts) {
        return attempts.stream().map(attempt -> {
            Exam exam = getExamById(attempt.getExamId());
            Map<String, Object> courseInfo = getCourseInfo(exam.getCourseId());
            Map<String, Object> employeeInfo = getEmployeeInfo(attempt.getEmployeeId());

            // Calculate total marks
            List<Question> questions = questionRepository.findByExamId(exam.getId());
            Integer totalMarks = questions.stream().mapToInt(Question::getMarks).sum();

            // Calculate percentage and grade
            Double percentage = totalMarks > 0 ? (attempt.getScore() / totalMarks) * 100 : 0.0;
            String grade = calculateGrade(percentage);

            // Calculate correct and wrong answers
            int correctCount = 0;
            int wrongCount = 0;

            try {
                Map<String, Object> answers = objectMapper.readValue(
                        Optional.ofNullable(attempt.getAnswersJson()).orElse("{}"), Map.class);

                for (Question q : questions) {
                    Object provided = answers.get(String.valueOf(q.getId()));
                    if (provided instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Integer> selectedOptionIds = (List<Integer>) provided;
                        Set<Long> correctIds = q.getOptions().stream()
                                .filter(Option::isCorrect)
                                .map(Option::getId)
                                .collect(Collectors.toSet());
                        Set<Long> selected = selectedOptionIds.stream().map(Integer::longValue).collect(Collectors.toSet());

                        if (correctIds.equals(selected)) correctCount++;
                        else wrongCount++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // ✅ Use the properly extracted employee name
            String employeeName = (String) employeeInfo.get("employeeName");
            String employeeEmail = (String) employeeInfo.get("employeeEmail");

            System.out.println("🎯 Final - Employee ID: " + attempt.getEmployeeId() + ", Name: " + employeeName);

            return ExamResultDTO.builder()
                    .attemptId(attempt.getId())
                    .examId(exam.getId())
                    .examTitle(exam.getTitle())
                    .employeeId(attempt.getEmployeeId())
                    .employeeName(employeeName) // This should now show actual names
                    .employeeEmail(employeeEmail)
                    .courseId(exam.getCourseId())
                    .courseName((String) courseInfo.get("courseName"))
                    .score(attempt.getScore())
                    .totalMarks(totalMarks)
                    .status(attempt.getStatus())
                    .startedAt(attempt.getStartedAt())
                    .submittedAt(attempt.getSubmittedAt())
                    .feedback(attempt.getFeedback())
                    .timeTakenMinutes(calculateTimeTakenMinutes(attempt))
                    .correctAnswers(correctCount)
                    .wrongAnswers(wrongCount)
                    .percentage(percentage)
                    .grade(grade)
                    .build();
        }).collect(Collectors.toList());
    }

    private Integer calculateTimeTakenMinutes(ExamAttempt attempt) {
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            return (int) Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMinutes();
        }
        return null;
    }

    private String calculateGrade(Double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    private String calculatePerformanceLevel(Double averageScore) {
        if (averageScore >= 80) return "EXCELLENT";
        if (averageScore >= 70) return "GOOD";
        if (averageScore >= 60) return "AVERAGE";
        return "POOR";
    }

    private String calculateDifficultyLevel(Double averageScore) {
        if (averageScore >= 80) return "EASY";
        if (averageScore >= 60) return "MEDIUM";
        return "HARD";
    }

    private String calculateEffectivenessRating(Double passRate, Double completionRate) {
        double rating = (passRate + completionRate) / 2;
        if (rating >= 0.8) return "HIGH";
        if (rating >= 0.6) return "MEDIUM";
        return "LOW";
    }

    // ✅ FIXED: Correct course progress lookup
    private Integer getCourseProgress(Long employeeId, Long courseId) {
        try {
            String enrollmentUrl = courseServiceUrl + "/courses/enrollments/" + employeeId;
            Map<?, ?> enrollmentResp = restTemplate.getForObject(enrollmentUrl, Map.class);
            List<?> enrollments = enrollmentResp != null ? (List<?>) enrollmentResp.get("data") : Collections.emptyList();

            // 🆕 COMPREHENSIVE DEBUGGING
            System.out.println("=== COURSE PROGRESS DEBUG ===");
            System.out.println("🔍 Employee: " + employeeId + ", Course: " + courseId);
            System.out.println("🔍 Enrollment URL: " + enrollmentUrl);
            System.out.println("🔍 Raw Response: " + enrollmentResp);
            System.out.println("🔍 Enrollments Count: " + enrollments.size());

            Integer progress = 0;
            boolean foundEnrollment = false;

            for (Object enrollmentObj : enrollments) {
                if (enrollmentObj instanceof Map) {
                    Map<?, ?> enrollmentMap = (Map<?, ?>) enrollmentObj;

                    System.out.println("🔍 Checking Enrollment: " + enrollmentMap);
                    System.out.println("🔍 Enrollment Keys: " + enrollmentMap.keySet());

                    // Try to find courseId in different possible field names
                    Object courseIdObj = enrollmentMap.get("courseId");
                    if (courseIdObj == null) {
                        courseIdObj = enrollmentMap.get("courseID");
                    }
                    if (courseIdObj == null) {
                        // Try nested course object
                        Object courseObj = enrollmentMap.get("course");
                        if (courseObj instanceof Map) {
                            Map<?, ?> courseMap = (Map<?, ?>) courseObj;
                            courseIdObj = courseMap.get("id");
                        }
                    }

                    if (courseIdObj != null && ((Number) courseIdObj).longValue() == courseId) {
                        foundEnrollment = true;
                        System.out.println("✅ FOUND MATCHING ENROLLMENT FOR COURSE: " + courseId);

                        // Try to extract progress from different possible field names
                        Object progressObj = enrollmentMap.get("progress");
                        if (progressObj == null) {
                            progressObj = enrollmentMap.get("completionPercentage");
                        }
                        if (progressObj == null) {
                            // Try nested course object for progress
                            Object courseObj = enrollmentMap.get("course");
                            if (courseObj instanceof Map) {
                                Map<?, ?> courseMap = (Map<?, ?>) courseObj;
                                progressObj = courseMap.get("progress");
                            }
                        }

                        System.out.println("📊 Progress Object Found: " + progressObj);
                        System.out.println("📊 Progress Object Type: " + (progressObj != null ? progressObj.getClass().getSimpleName() : "null"));

                        if (progressObj instanceof Number) {
                            progress = ((Number) progressObj).intValue();
                            System.out.println("🎯 EXTRACTED PROGRESS: " + progress + "%");
                        } else {
                            System.out.println("❌ Progress is not a number: " + progressObj);
                            progress = 0;
                        }
                        break;
                    }
                }
            }

            if (!foundEnrollment) {
                System.out.println("❌ NO ENROLLMENT FOUND for course: " + courseId);
            }

            System.out.println("🎯 FINAL PROGRESS: " + progress + "%");
            System.out.println("=== END DEBUG ===");

            return progress;

        } catch (Exception e) {
            System.err.println("❌ ERROR getting course progress for employee " + employeeId + " in course " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getEmployeeEnrollments(Long employeeId) {
        try {
            String enrollmentUrl = courseServiceUrl + "/courses/enrollments/" + employeeId;
            Map<?, ?> enrollmentResp = restTemplate.getForObject(enrollmentUrl, Map.class);
            return enrollmentResp != null ? (List<Map<String, Object>>) enrollmentResp.get("data") : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Integer getCourseEnrollmentsCount(Long courseId) {
        try {
            String enrollmentUrl = courseServiceUrl + "/courses/" + courseId + "/enrollments/count";
            Map<?, ?> countResp = restTemplate.getForObject(enrollmentUrl, Map.class);
            return countResp != null && countResp.get("data") != null ?
                    ((Number) countResp.get("data")).intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCourseInfo(Long courseId) {
        try {
            String courseUrl = courseServiceUrl + "/courses/" + courseId;
            Map<String, Object> courseResp = restTemplate.getForObject(courseUrl, Map.class);
            if (courseResp != null && courseResp.get("data") instanceof Map) {
                return (Map<String, Object>) courseResp.get("data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Map.of("courseName", "Unknown Course");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getEmployeeInfo(Long employeeId) {
        try {
            String userUrl = userServiceUrl + "/api/users/" + employeeId;
            Map<String, Object> userResp = restTemplate.getForObject(userUrl, Map.class);
            System.out.println("🔍 Fetching employee info for ID: " + employeeId);
            System.out.println("📡 User Service Response: " + userResp);

            if (userResp != null && userResp.get("data") instanceof Map) {
                Map<String, Object> employeeData = (Map<String, Object>) userResp.get("data");

                // ✅ CORRECTED: Extract firstName and lastName from the nested data
                String firstName = (String) employeeData.get("firstName");
                String lastName = (String) employeeData.get("lastName");
                String email = (String) employeeData.get("email");

                System.out.println("👤 Extracted - First: " + firstName + ", Last: " + lastName + ", Email: " + email);

                // Build employee name
                String employeeName;
                if (firstName != null && lastName != null) {
                    employeeName = firstName + " " + lastName;
                } else if (firstName != null) {
                    employeeName = firstName;
                } else if (lastName != null) {
                    employeeName = lastName;
                } else {
                    employeeName = "Employee " + employeeId;
                }

                return Map.of(
                        "employeeName", employeeName,
                        "employeeEmail", email != null ? email : "unknown@example.com",
                        "department", employeeData.getOrDefault("department", "Unknown Department"),
                        "firstName", firstName,
                        "lastName", lastName
                );
            } else {
                System.out.println("⚠️ No employee data found for ID: " + employeeId);
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching employee info for ID: " + employeeId);
            e.printStackTrace();
        }

        // Return default values if employee not found
        return Map.of(
                "employeeName", "Employee " + employeeId,
                "employeeEmail", "unknown@example.com",
                "department", "Unknown Department",
                "firstName", null,
                "lastName", null
        );
    }

    // NEW HELPER METHODS FOR MANAGER FILTERING
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getManagerTeamEmployees(Long managerId) {
        try {
            String teamUrl = userServiceUrl + "/api/users/manager/" + managerId + "/team";
            Map<?, ?> teamResp = restTemplate.getForObject(teamUrl, Map.class);
            return teamResp != null ? (List<Map<String, Object>>) teamResp.get("data") : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllEmployees() {
        try {
            String employeesUrl = userServiceUrl + "/api/users/role/EMPLOYEE";
            Map<?, ?> employeesResp = restTemplate.getForObject(employeesUrl, Map.class);
            return employeesResp != null ? (List<Map<String, Object>>) employeesResp.get("data") : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void sendAnalyticsReport(ExamAttempt attempt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("employeeId", attempt.getEmployeeId());
            payload.put("examId", attempt.getExamId());
            payload.put("averageMarks", attempt.getScore());
            payload.put("attempts", 1);

            restTemplate.postForObject(analyticsServiceUrl + "/api/analytics", payload, String.class);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private String fetchUserRole(Long userId) {
        try {
            if (userId == null) return null;
            String url = userServiceUrl + "/api/users/" + userId;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return null;
            Object data = resp.get("data");
            if (data instanceof Map) {
                Object role = ((Map<String, Object>) data).get("role");
                return role != null ? role.toString() : null;
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    private void notifyAdminsAboutManagerExam(Long managerId, Exam exam) {
        try {
            String urlAdmins = userServiceUrl + "/api/users/role/ADMIN";
            Map<String, Object> respAdmins = restTemplate.getForObject(urlAdmins, Map.class);
            if (respAdmins != null && respAdmins.get("data") instanceof List) {
                List<?> admins = (List<?>) respAdmins.get("data");
                for (Object a : admins) {
                    if (a instanceof Map) {
                        Object email = ((Map<?, ?>) a).get("email");
                        if (email != null) {
                            Map<String, String> payload = new HashMap<>();
                            payload.put("to", email.toString());
                            payload.put("subject", "New Exam Created by Manager");
                            payload.put("message", "Manager created a new exam: " + exam.getTitle());
                            restTemplate.postForObject(notificationServiceUrl, payload, String.class);
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}