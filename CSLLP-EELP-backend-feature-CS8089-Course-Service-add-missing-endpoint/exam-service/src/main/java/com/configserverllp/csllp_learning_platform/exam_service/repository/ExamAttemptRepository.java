package com.configserverllp.csllp_learning_platform.exam_service.repository;

import com.configserverllp.csllp_learning_platform.exam_service.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByExamIdAndEmployeeId(Long examId, Long employeeId);

    // 2️⃣ All attempts for a specific exam
    List<ExamAttempt> findByExamId(Long examId);

    // 3️⃣ All attempts for a specific employee
    List<ExamAttempt> findByEmployeeId(Long employeeId);

}
