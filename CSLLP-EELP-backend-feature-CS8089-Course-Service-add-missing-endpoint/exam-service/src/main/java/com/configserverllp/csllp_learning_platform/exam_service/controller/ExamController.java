package com.configserverllp.csllp_learning_platform.exam_service.controller;

import com.configserverllp.csllp_learning_platform.exam_service.dto.*;
import com.configserverllp.csllp_learning_platform.exam_service.entity.*;
import com.configserverllp.csllp_learning_platform.exam_service.service.ExamService;
import com.configserverllp.csllp_learning_platform.exam_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<ApiResponse<Exam>> createExam(
            @RequestBody ExamRequest req,
            @RequestHeader("X-Creator-Id") Long creatorId) {

        return ResponseEntity.ok(
                ApiResponse.success("Exam created", examService.createExam(req, creatorId))
        );
    }

    // Get exam by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Exam>> getExam(@PathVariable Long id) {
        Exam e = examService.getExamById(id);
        return ResponseEntity.ok(ApiResponse.success("Exam fetched", e));
    }

    // Get all exams
    @GetMapping
    public ResponseEntity<ApiResponse<List<Exam>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Exams fetched", examService.getAllExams()));
    }

    // Update exam
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Exam>> updateExam(@PathVariable Long id, @Valid @RequestBody ExamRequest req) {
        Exam updated = examService.updateExam(id, req);
        return ResponseEntity.ok(ApiResponse.success("Exam updated", updated));
    }

    // Soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok(ApiResponse.success("Exam deleted (soft)", null));
    }

    // Add question to exam
    @PostMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<Question>> addQuestion(
            @PathVariable Long examId,
            @Valid @RequestBody QuestionRequest req) {
        Question q = examService.addQuestion(examId, req);
        return ResponseEntity.status(201).body(ApiResponse.success("Question added", q));
    }

    // Get questions for exam
    @GetMapping("/{examId}/questions")
    public ResponseEntity<ApiResponse<List<Question>>> getQuestions(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success("Questions fetched", examService.getQuestionsByExam(examId)));
    }

    // Start attempt
    @PostMapping("/{examId}/start")
    public ResponseEntity<ApiResponse<ExamAttempt>> startAttempt(@PathVariable Long examId,
                                                                 @RequestParam Long employeeId) {
        ExamAttempt attempt = examService.startAttempt(examId, employeeId);
        return ResponseEntity.status(201).body(ApiResponse.success("Attempt started", attempt));
    }

   /* // Submit attempt (answers)
    @PostMapping("/{examId}/submit")
    public ResponseEntity<ApiResponse<ExamAttempt>> submitAttempt(@PathVariable Long examId,
                                                                  @Valid @RequestBody SubmitAttemptRequest req) {
        ExamAttempt result = examService.submitAttempt(examId, req);
        return ResponseEntity.ok(ApiResponse.success("Attempt submitted", result));
    }*/
   /*@PostMapping("/{examId}/submit")
   public ResponseEntity<ApiResponse<ExamAttempt>> submitAttempt(
           @PathVariable Long examId,
           @Valid @RequestBody SubmitAttemptRequest req) {

       // 1️⃣ Submit the exam and calculate result
       ExamAttempt result = examService.submitAttempt(examId, req);

       // 2️⃣ Check if employee passed
       if(result.getScore() >= 50) {  // adjust passing score as needed
           try {
               // Get employeeId and courseId
               Long employeeId = req.getEmployeeId();
               Long courseId = examService.getCourseIdByExamId(examId);

               // Call Certification Service API
               String url = "http://localhost:8084/api/certifications/auto-generate/{employeeId}/{courseId}";
               Certification cert = examService.generateCertificate(employeeId, courseId, url);

               // Optional: add certificate URL to response
               result.setCertificateUrl("http://localhost:8084/api/certifications/employee/"
                       + employeeId + "/download/" + cert.getId());

           } catch (Exception e) {
               e.printStackTrace();
           }
       }

       return ResponseEntity.ok(ApiResponse.success("Attempt submitted", result));
   }*/
   @PostMapping("/{examId}/submit")
   public ResponseEntity<ApiResponse<ExamAttempt>> submitAttempt(
           @PathVariable Long examId,
           @Valid @RequestBody SubmitAttemptRequest req) {

       // 1️⃣ Submit the exam attempt
       ExamAttempt result = examService.submitAttempt(examId, req);

       // 2️⃣ Optional: add certificate URL if generated
       if(result.getCertificateUrl() != null) {
           return ResponseEntity.ok(ApiResponse.success("Attempt submitted. Certificate generated!", result));
       }

       return ResponseEntity.ok(ApiResponse.success("Attempt submitted", result));
   }

    // Get attempt/result
    @GetMapping("/{examId}/attempts/{employeeId}")
    public ResponseEntity<ApiResponse<ExamAttempt>> getAttempt(@PathVariable Long examId,
                                                               @PathVariable Long employeeId) {
        ExamAttempt attempt = examService.getAttempt(examId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Attempt fetched", attempt));
    }

    // Get exam by course (frontend uses this after course completion)
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<Exam>> getExamByCourse(@PathVariable Long courseId) {
        Exam exam = examService.getExamByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.success("Exam for course fetched", exam));
    }

    // ========== RESULTS & REPORTING ENDPOINTS ==========

    // 1️⃣ Get all exam results (all attempts) - For Admins/Managers
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<List<ExamResultDTO>>> getAllResults() {
        List<ExamResultDTO> results = examService.getAllExamResults();
        return ResponseEntity.ok(ApiResponse.success("All exam results fetched", results));
    }

    // 2️⃣ Get results for a specific exam - For Admins/Managers
    @GetMapping("/{examId}/results")
    public ResponseEntity<ApiResponse<List<ExamResultDTO>>> getResultsByExam(@PathVariable Long examId) {
        List<ExamResultDTO> results = examService.getExamResultsByExam(examId);
        return ResponseEntity.ok(ApiResponse.success("Results fetched for exam", results));
    }

    // 3️⃣ Get results for a specific employee (for "My Results" section) - For Employees
    @GetMapping("/results/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeResultDTO>>> getEmployeeResults(@PathVariable Long employeeId) {
        List<EmployeeResultDTO> results = examService.getEmployeeResults(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee results fetched", results));
    }

    // 4️⃣ Get detailed employee performance report - For Admins/Managers
    @GetMapping("/reports/employee-performance")
    public ResponseEntity<ApiResponse<List<EmployeePerformanceReportDTO>>> getEmployeePerformanceReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long courseId) {
        List<EmployeePerformanceReportDTO> report = examService.getEmployeePerformanceReport(employeeId, courseId);
        return ResponseEntity.ok(ApiResponse.success("Employee performance report generated", report));
    }

    // 5️⃣ Get course effectiveness report - For Admins/Managers
    @GetMapping("/reports/course-effectiveness")
    public ResponseEntity<ApiResponse<List<CourseEffectivenessReportDTO>>> getCourseEffectivenessReport() {
        List<CourseEffectivenessReportDTO> report = examService.getCourseEffectivenessReport();
        return ResponseEntity.ok(ApiResponse.success("Course effectiveness report generated", report));
    }

    // ========== EMPLOYEE SPECIFIC ENDPOINTS ==========

    // Get exams for employee with enrollment status
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeExamDTO>>> getExamsForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Exams for employee fetched",
                examService.getExamsForEmployee(employeeId)));
    }

    // Check exam eligibility for employee
    @GetMapping("/{examId}/eligibility/{employeeId}")
    public ResponseEntity<ApiResponse<ExamEligibilityDTO>> checkExamEligibility(
            @PathVariable Long examId,
            @PathVariable Long employeeId) {
        ExamEligibilityDTO eligibility = examService.checkExamEligibility(examId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Exam eligibility checked", eligibility));
    }

    // ========== ANALYTICS ENDPOINTS ==========

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<List<AnalyticsReportDto>>> getAnalytics(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched",
                examService.getAnalytics(courseId, employeeId, fromDate, toDate)));
    }
// ========== NEW ENDPOINTS FOR REPORTING ==========

    // Get pending exams count for employee
    @GetMapping("/pending-count/{employeeId}")
    public ResponseEntity<ApiResponse<Long>> getPendingExamsCount(@PathVariable Long employeeId) {
        Long count = examService.getPendingExamsCount(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Pending exams count fetched", count));
    }

    // Get total employees count (for admin)
    @GetMapping("/employees/count")
    public ResponseEntity<ApiResponse<Long>> getTotalEmployeesCount() {
        Long count = examService.getTotalEmployeesCount();
        return ResponseEntity.ok(ApiResponse.success("Total employees count fetched", count));
    }

    // Get manager's team results
    @GetMapping("/reports/manager-team/{managerId}")
    public ResponseEntity<ApiResponse<List<ExamResultDTO>>> getManagerTeamResults(@PathVariable Long managerId) {
        List<ExamResultDTO> results = examService.getManagerTeamResults(managerId);
        return ResponseEntity.ok(ApiResponse.success("Manager team results fetched", results));
    }

}