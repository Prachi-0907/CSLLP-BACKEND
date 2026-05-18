package com.configserverllp.csllp_learning_platform.submission_service.repository;

import com.configserverllp.csllp_learning_platform.submission_service.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByExamId(Long examId);
    List<Submission> findByUserId(Long userId);
}
