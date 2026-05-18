package com.configserverllp.course_service.repository;

import com.configserverllp.course_service.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByEmployeeId(Long employeeId);

    List<Assignment> findByCourseId(Long courseId);

    List<Assignment> findByStatus(Assignment.Status status);

    List<Assignment> findByEmployeeIdAndStatus(Long employeeId, Assignment.Status status);

    List<Assignment> findByCourseIdAndStatus(Long courseId, Assignment.Status status);

    // ADD THIS METHOD FOR DUPLICATE CHECK:
    List<Assignment> findByEmployeeIdAndCourseId(Long employeeId, Long courseId);
}
