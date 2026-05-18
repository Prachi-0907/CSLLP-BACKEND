package com.configserverllp.csllp_learning_platform.submission_service.service;

import com.configserverllp.csllp_learning_platform.submission_service.dto.SubmissionDto;

import java.util.List;

public interface SubmissionService {
    SubmissionDto createSubmission(SubmissionDto submissionDto);
    List<SubmissionDto> getAllSubmissions();
    SubmissionDto getSubmissionById(Long id);
    List<SubmissionDto> getSubmissionsByExam(Long examId);
    List<SubmissionDto> getSubmissionsByUser(Long userId);
    SubmissionDto updateSubmission(Long id, SubmissionDto submissionDto);
    void deleteSubmission(Long id);
}
