package com.configserverllp.csllp_learning_platform.certification_service.service;

import com.configserverllp.csllp_learning_platform.certification_service.dto.CertificationRequest;
import com.configserverllp.csllp_learning_platform.certification_service.entity.Certification;
import org.springframework.core.io.Resource;
import java.util.List;

public interface CertificationService {
    Certification generateCertificate(CertificationRequest request, Long creatorId);
    Certification autoGenerateCertificate(Long employeeId, Long courseId);
    List<Certification> getAllCertifications(String employeeName, String courseName, String status);
    List<Certification> getEmployeeCertificates(Long employeeId);
    Resource generatePdfCertificate(Long certificationId);
    Resource downloadEmployeeCertificate(Long employeeId, Long certificationId);
    Certification revokeCertificate(Long certificationId, Long creatorId);
    void checkAndUpdateExpiredCertificates();
    boolean isEmployeeEligibleForCertificate(Long employeeId, Long courseId);
}