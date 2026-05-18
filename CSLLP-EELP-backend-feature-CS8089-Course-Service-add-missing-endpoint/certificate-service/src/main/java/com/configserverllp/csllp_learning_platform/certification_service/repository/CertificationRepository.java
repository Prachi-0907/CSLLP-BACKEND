package com.configserverllp.csllp_learning_platform.certification_service.repository;

import com.configserverllp.csllp_learning_platform.certification_service.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByEmployeeId(Long employeeId);
    List<Certification> findByEmployeeIdAndStatus(Long employeeId, String status);
    List<Certification> findByCourseId(Long courseId);
    List<Certification> findByStatus(String status);
    Optional<Certification> findByEmployeeIdAndCourseIdAndStatus(Long employeeId, Long courseId, String status);
    Optional<Certification> findByIdAndEmployeeId(Long id, Long employeeId);
}