package com.configserverllp.csllp_learning_platform.submission_service.service.impl;

import com.configserverllp.csllp_learning_platform.submission_service.dto.SubmissionDto;
import com.configserverllp.csllp_learning_platform.submission_service.entity.Submission;
import com.configserverllp.csllp_learning_platform.submission_service.repository.SubmissionRepository;
import com.configserverllp.csllp_learning_platform.submission_service.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;

    @Autowired
    public SubmissionServiceImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Override
    public SubmissionDto createSubmission(SubmissionDto submissionDto) {
        Submission submission = new Submission();
        submission.setExamId(submissionDto.getExamId());
        submission.setUserId(submissionDto.getUserId());
        submission.setAnswer(submissionDto.getAnswer());
        submission.setMarks(submissionDto.getMarks());
        submission.setSubmittedAt(LocalDateTime.now());

        Submission saved = submissionRepository.save(submission);
        submissionDto.setId(saved.getId());
        submissionDto.setSubmittedAt(saved.getSubmittedAt());
        return submissionDto;
    }

    @Override
    public List<SubmissionDto> getAllSubmissions() {
        return submissionRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public SubmissionDto getSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + id));
    }

    @Override
    public List<SubmissionDto> getSubmissionsByExam(Long examId) {
        return submissionRepository.findByExamId(examId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<SubmissionDto> getSubmissionsByUser(Long userId) {
        return submissionRepository.findByUserId(userId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public SubmissionDto updateSubmission(Long id, SubmissionDto submissionDto) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with id: " + id));

        submission.setAnswer(submissionDto.getAnswer());
        submission.setMarks(submissionDto.getMarks());

        Submission updated = submissionRepository.save(submission);
        return mapToDto(updated);
    }

    @Override
    public void deleteSubmission(Long id) {
        submissionRepository.deleteById(id);
    }

    private SubmissionDto mapToDto(Submission submission) {
        SubmissionDto dto = new SubmissionDto();
        dto.setId(submission.getId());
        dto.setExamId(submission.getExamId());
        dto.setUserId(submission.getUserId());
        dto.setAnswer(submission.getAnswer());
        dto.setMarks(submission.getMarks());
        dto.setSubmittedAt(submission.getSubmittedAt());
        return dto;
    }
}
