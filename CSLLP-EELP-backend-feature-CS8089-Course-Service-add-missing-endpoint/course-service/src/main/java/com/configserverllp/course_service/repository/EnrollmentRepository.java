package com.configserverllp.course_service.repository;

import com.configserverllp.course_service.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByCourseIdAndEmployeeId(Long courseId, Long employeeId);

    List<Enrollment> findByEmployeeId(Long employeeId);

    //Get enrollments for a specific course
    List<Enrollment> findByCourseId(Long courseId);

    // 🆕 ADD THIS METHOD - Find enrollments older than X days with progress < 50%
    @Query("SELECT e FROM Enrollment e WHERE e.enrolledAt <= :cutoffDate AND e.progress < 50")
    List<Enrollment> findEnrollmentsNeedingReminders(@Param("cutoffDate") LocalDateTime cutoffDate);
}
