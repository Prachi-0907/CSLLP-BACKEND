package com.configserverllp.csllp_learning_platform.exam_service.repository;

import com.configserverllp.csllp_learning_platform.exam_service.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByCourseId(Long courseId);
}
