package com.configserverllp.csllp_learning_platform.exam_service.service;

import com.configserverllp.csllp_learning_platform.exam_service.dto.*;
import com.configserverllp.csllp_learning_platform.exam_service.entity.*;

import java.util.List;

public interface ExamService {
    Exam createExam(ExamRequest req, Long creatorId);
    Exam updateExam(Long id, ExamRequest req);
    void deleteExam(Long id);
    Exam getExamById(Long id);
    List<Exam> getAllExams();
    Exam getExamByCourse(Long courseId);

    Question addQuestion(Long examId, QuestionRequest req);
    List<Question> getQuestionsByExam(Long examId);

    ExamAttempt startAttempt(Long examId, Long employeeId);
    ExamAttempt submitAttempt(Long examId, SubmitAttemptRequest req);
    ExamAttempt getAttempt(Long examId, Long employeeId);

    // Results and Reporting
    List<ExamResultDTO> getAllExamResults();
    List<ExamResultDTO> getExamResultsByExam(Long examId);

    List<EmployeeResultDTO> getEmployeeResults(Long employeeId);

    // Employee specific
    List<EmployeeExamDTO> getExamsForEmployee(Long employeeId);
    ExamEligibilityDTO checkExamEligibility(Long examId, Long employeeId);

    // Reports for Admins/Managers
    List<EmployeePerformanceReportDTO> getEmployeePerformanceReport(Long employeeId, Long courseId);
    List<CourseEffectivenessReportDTO> getCourseEffectivenessReport();

    // Analytics
    List<AnalyticsReportDto> getAnalytics(Long courseId, Long employeeId, String fromDate, String toDate);

    // NEW METHODS FOR REPORTING
    Long getPendingExamsCount(Long employeeId);
    Long getTotalEmployeesCount();
    List<ExamResultDTO> getManagerTeamResults(Long managerId);
}