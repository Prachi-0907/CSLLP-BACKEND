package com.configserverllp.csllp_learning_platform.certification_service.controller;


import com.configserverllp.csllp_learning_platform.certification_service.dto.CertificationRequest;
import com.configserverllp.csllp_learning_platform.certification_service.entity.Certification;
import com.configserverllp.csllp_learning_platform.certification_service.service.CertificationService;
import com.configserverllp.csllp_learning_platform.certification_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    // ========== ADMIN/MANAGER ENDPOINTS ==========

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Certification>> generateCertificate(
            @RequestBody CertificationRequest request,
            @RequestHeader("X-Creator-Id") Long creatorId) {
        Certification cert = certificationService.generateCertificate(request, creatorId);
        return ResponseEntity.ok(ApiResponse.success("Certificate generated", cert));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<Certification>>> getAllCertifications(
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String status) {
        List<Certification> certs = certificationService.getAllCertifications(employeeName, courseName, status);
        return ResponseEntity.ok(ApiResponse.success("Certifications fetched", certs));
    }

    @GetMapping("/admin/{id}/download")
    public ResponseEntity<Resource> downloadCertificateAdmin(@PathVariable Long id) {
        Resource pdfResource = certificationService.generatePdfCertificate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate_" + id + ".pdf\"")
                .body(pdfResource);
    }

    @PutMapping("/admin/{id}/revoke")
    public ResponseEntity<ApiResponse<Certification>> revokeCertificate(
            @PathVariable Long id,
            @RequestHeader("X-Creator-Id") Long creatorId) {
        Certification cert = certificationService.revokeCertificate(id, creatorId);
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked", cert));
    }

    // ========== EMPLOYEE ENDPOINTS ==========

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<Certification>>> getEmployeeCertificates(@PathVariable Long employeeId) {
        List<Certification> certs = certificationService.getEmployeeCertificates(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Your certificates fetched", certs));
    }

    @GetMapping("/employee/{employeeId}/download/{certificationId}")
    public ResponseEntity<Resource> downloadCertificateEmployee(
            @PathVariable Long employeeId,
            @PathVariable Long certificationId) {
        Resource pdfResource = certificationService.downloadEmployeeCertificate(employeeId, certificationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate_" + certificationId + ".pdf\"")
                .body(pdfResource);
    }

    // ========== AUTOMATIC GENERATION ENDPOINT ==========

    @PostMapping("/auto-generate/{employeeId}/{courseId}")
    public ResponseEntity<ApiResponse<Certification>> autoGenerateCertificate(
            @PathVariable Long employeeId,
            @PathVariable Long courseId) {
        Certification cert = certificationService.autoGenerateCertificate(employeeId, courseId);
        return ResponseEntity.ok(ApiResponse.success("Certificate auto-generated", cert));
    }
}