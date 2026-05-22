package com.configserverllp.csllp_learning_platform.certification_service.service.impl;

import com.configserverllp.csllp_learning_platform.certification_service.dto.CertificationRequest;
import com.configserverllp.csllp_learning_platform.certification_service.entity.Certification;
import com.configserverllp.csllp_learning_platform.certification_service.exception.BadRequestException;
import com.configserverllp.csllp_learning_platform.certification_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.certification_service.repository.CertificationRepository;
import com.configserverllp.csllp_learning_platform.certification_service.service.CertificationService;
import com.configserverllp.csllp_learning_platform.certification_service.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements CertificationService {

    private final CertificationRepository certificationRepository;
    private final RestTemplate restTemplate;
    private final PdfGenerator pdfGenerator;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${course.service.url}")
    private String courseServiceUrl;

    @Value("${exam.service.url}")
    private String examServiceUrl;

    @Value("${certificate.storage.path}")
    private String certificateStoragePath;

    @Override
    public Certification generateCertificate(CertificationRequest request, Long creatorId) {
        // Validate creator role (Admin/Manager)
        String creatorRole = fetchUserRole(creatorId);
        if (!"ADMIN".equalsIgnoreCase(creatorRole) && !"MANAGER".equalsIgnoreCase(creatorRole)) {
            throw new BadRequestException("Only ADMIN or MANAGER can generate certificates");
        }

        // Check if employee is eligible
        if (!isEmployeeEligibleForCertificate(request.getEmployeeId(), request.getCourseId())) {
            throw new BadRequestException("Employee is not eligible for certificate");
        }

        // Check for existing active certificate
        certificationRepository.findByEmployeeIdAndCourseIdAndStatus(
                        request.getEmployeeId(), request.getCourseId(), "ACTIVE")
                .ifPresent(cert -> {
                    throw new BadRequestException("Active certificate already exists for this course");
                });

        Certification certification = Certification.builder()
                .employeeId(request.getEmployeeId())
                .courseId(request.getCourseId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now())
                .expiryDate(request.getExpiryDate())
                .status("ACTIVE")
                .build();

        Certification savedCert = certificationRepository.save(certification);

        // Generate PDF certificate
        generateAndStorePdfCertificate(savedCert);

        return savedCert;
    }

    @Override
    public Certification autoGenerateCertificate(Long employeeId, Long courseId) {
        System.out.println("🎯 Starting auto-certificate generation for employee: " + employeeId + ", course: " + courseId);

        // Auto-check eligibility
        if (!isEmployeeEligibleForCertificate(employeeId, courseId)) {
            System.out.println("❌ Employee not eligible for automatic certificate generation");
            throw new BadRequestException("Employee is not eligible for automatic certificate generation");
        }

        // Check if certificate already exists
        certificationRepository.findByEmployeeIdAndCourseIdAndStatus(employeeId, courseId, "ACTIVE")
                .ifPresent(cert -> {
                    System.out.println("⚠️ Active certificate already exists");
                    throw new BadRequestException("Active certificate already exists for this course");
                });

        System.out.println("✅ Creating new certificate...");
        Certification certification = Certification.builder()
                .employeeId(employeeId)
                .courseId(courseId)
                .issueDate(LocalDate.now())
                .status("ACTIVE")
                .build();

        Certification savedCert = certificationRepository.save(certification);
        System.out.println("✅ Certificate saved with ID: " + savedCert.getId());

        generateAndStorePdfCertificate(savedCert);
        System.out.println("✅ PDF certificate generated");

        return savedCert;
    }

    @Override
    public boolean isEmployeeEligibleForCertificate(Long employeeId, Long courseId) {
        try {
            System.out.println("🔍 Checking certificate eligibility for employee: " + employeeId + ", course: " + courseId);

            // Check if employee exists and is active
            Map<String, Object> employee = fetchEmployeeInfo(employeeId);
            if (!"ACTIVE".equals(employee.get("status"))) {
                System.out.println("❌ Employee not active");
                return false;
            }

            // Check course completion (100% progress)
            Integer progress = getCourseProgress(employeeId, courseId);
            System.out.println("📊 Course progress: " + progress + "%");
            if (progress < 100) {
                System.out.println("❌ Course not completed (progress: " + progress + "%)");
                return false;
            }

            // ✅ FIXED: Check if exam exists and was PASSED (not just eligibility)
            try {
                // Get exam by course
                System.out.println("🔍 Looking for exam for course: " + courseId);
                Map<String, Object> examResponse = restTemplate.getForObject(
                        examServiceUrl + "/api/exams/course/" + courseId, Map.class);

                if (examResponse != null && examResponse.get("data") != null) {
                    Map<String, Object> examData = (Map<String, Object>) examResponse.get("data");
                    Long examId = ((Number) examData.get("id")).longValue();
                    System.out.println("✅ Found exam: " + examId);

                    // Check exam attempt and score
                    Map<String, Object> attemptResponse = restTemplate.getForObject(
                            examServiceUrl + "/api/exams/" + examId + "/attempts/" + employeeId, Map.class);

                    if (attemptResponse != null && attemptResponse.get("data") != null) {
                        Map<String, Object> attemptData = (Map<String, Object>) attemptResponse.get("data");

                        // Check if exam was completed and passed (50% or higher)
                        String status = (String) attemptData.get("status");
                        Double score = attemptData.get("score") != null ?
                                ((Number) attemptData.get("score")).doubleValue() : 0.0;

                        System.out.println("📊 Exam status: " + status + ", score: " + score);

                        // Must be COMPLETED and have 50% or higher
                        if (!"COMPLETED".equals(status)) {
                            System.out.println("❌ Exam not completed, status: " + status);
                            return false;
                        }

                        // ✅ TEMPORARY: Lowered passing score for testing
                        if (score < 50.0) {
                            System.out.println("❌ Exam not passed, score: " + score);
                            return false;
                        }

                        System.out.println("✅ Exam passed! Eligible for certificate.");
                    } else {
                        System.out.println("❌ No exam attempt found");
                        return false;
                    }
                } else {
                    System.out.println("ℹ️ No exam found for course - certificate based on course completion only");
                    // If no exam exists, course completion is sufficient
                    return true;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Exam check failed: " + e.getMessage());
                // If exam service is unavailable, fall back to course completion only
                return true;
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Eligibility check error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Certification> getAllCertifications(String employeeName, String courseName, String status) {
        List<Certification> certifications = certificationRepository.findAll();

        // Apply filters
        if (employeeName != null) {
            // You would need to join with user service to filter by name
            // For now, this is a simplified version
        }

        if (courseName != null) {
            // Filter by course name (would need course service integration)
        }

        if (status != null) {
            certifications = certifications.stream()
                    .filter(cert -> status.equalsIgnoreCase(cert.getStatus()))
                    .toList();
        }

        return certifications;
    }

    @Override
    public List<Certification> getEmployeeCertificates(Long employeeId) {
        return certificationRepository.findByEmployeeIdAndStatus(employeeId, "ACTIVE");
    }

    @Override
    public Resource generatePdfCertificate(Long certificationId) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));

        try {
            Path certificatePath = Paths.get(certification.getCertificatePath());
            if (Files.exists(certificatePath)) {
                return new UrlResource(certificatePath.toUri());
            } else {
                // Regenerate if file doesn't exist
                return generateAndStorePdfCertificate(certification);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF certificate", e);
        }
    }

    @Override
    public Resource downloadEmployeeCertificate(Long employeeId, Long certificationId) {
        Certification certification = certificationRepository.findByIdAndEmployeeId(certificationId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found or access denied"));

        return generatePdfCertificate(certificationId);
    }

    @Override
    public Certification revokeCertificate(Long certificationId, Long creatorId) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));

        String creatorRole = fetchUserRole(creatorId);
        if (!"ADMIN".equalsIgnoreCase(creatorRole)) {
            throw new BadRequestException("Only ADMIN can revoke certificates");
        }

        certification.setStatus("REVOKED");
        return certificationRepository.save(certification);
    }

    @Override
    public void checkAndUpdateExpiredCertificates() {
        List<Certification> activeCerts = certificationRepository.findByStatus("ACTIVE");
        LocalDate today = LocalDate.now();

        for (Certification cert : activeCerts) {
            if (cert.getExpiryDate() != null && cert.getExpiryDate().isBefore(today)) {
                cert.setStatus("EXPIRED");
                certificationRepository.save(cert);
            }
        }
    }

   /* private Resource generateAndStorePdfCertificate(Certification certification) {
        try {
            // Fetch employee and course details
            Map<String, Object> employeeInfo = fetchEmployeeInfo(certification.getEmployeeId());
            Map<String, Object> courseInfo = fetchCourseInfo(certification.getCourseId());

            // 🔍 Debug logs to verify data fetched
            System.out.println("👤 Employee Info: " + employeeInfo);
            System.out.println("📘 Course Info: " + courseInfo);

            // Prepare data for PDF with better name extraction
            Map<String, Object> pdfData = new HashMap<>();

            // ✅ Extract employee name properly
            String employeeName = extractEmployeeName(employeeInfo);
            String employeeEmail = extractEmployeeEmail(employeeInfo);
            String courseName = extractCourseName(courseInfo);
            String courseDescription = extractCourseDescription(courseInfo);

            pdfData.put("employeeName", employeeName);
            pdfData.put("employeeEmail", employeeEmail);
            pdfData.put("courseName", courseName);
            pdfData.put("courseDescription", courseDescription);
            pdfData.put("issueDate", certification.getIssueDate());
            pdfData.put("expiryDate", certification.getExpiryDate());
            pdfData.put("verificationCode", certification.getVerificationCode());

            // Debug the final data
            System.out.println("=== FINAL PDF DATA ===");
            System.out.println("Employee Name: " + employeeName);
            System.out.println("Employee Email: " + employeeEmail);
            System.out.println("Course Name: " + courseName);
            System.out.println("Course Description: " + courseDescription);

            // ✅ Ensure certificate storage directory exists
            Path certificatesDir = Paths.get(certificateStoragePath);
            System.out.println("📁 Certificate storage path: " + certificatesDir.toAbsolutePath());

            if (!Files.exists(certificatesDir)) {
                Files.createDirectories(certificatesDir);
            }

            // Generate PDF
            String fileName = "certificate_" + certification.getId() + ".pdf";
            Path filePath = certificatesDir.resolve(fileName);

            System.out.println("📄 Generating PDF for: " + employeeName + " - " + courseName);
            pdfGenerator.generateCertificatePdf(pdfData, filePath.toString());

            // Update certificate path
            certification.setCertificatePath(filePath.toString());
            certificationRepository.save(certification);

            System.out.println("✅ PDF certificate generated successfully for: " + employeeName);
            return new UrlResource(filePath.toUri());

        } catch (Exception e) {
            System.err.println("❌ Failed to generate PDF certificate: " + e.getMessage());
            throw new RuntimeException("Failed to generate PDF certificate", e);
        }
    }*/

    private Resource generateAndStorePdfCertificate(Certification certification) {
        try {
            // Fetch employee and course details
            Map<String, Object> employeeInfo = fetchEmployeeInfo(certification.getEmployeeId());
            Map<String, Object> courseInfo = fetchCourseInfo(certification.getCourseId());

            // 🔍 Enhanced debug logs
            System.out.println("=== CERTIFICATE DATA DEBUG ===");
            System.out.println("👤 Raw Employee Info: " + employeeInfo);
            System.out.println("📘 Raw Course Info: " + courseInfo);

//            // ✅ Enhanced employee name extraction with multiple fallbacks
//            String employeeName = extractEmployeeName(employeeInfo);
//            String courseName = extractCourseName(courseInfo);
            // Extract first and last name separately from employeeInfo
//            String firstName = (String) employeeInfo.get("firstName");
//            String lastName = (String) employeeInfo.get("lastName");
            String firstName = employeeInfo.get("firstName") != null
                    ? employeeInfo.get("firstName").toString()
                    : "Unknown";

            String lastName = employeeInfo.get("lastName") != null
                    ? employeeInfo.get("lastName").toString()
                    : "";
            String courseName = extractCourseName(courseInfo);


            //System.out.println("✅ Extracted Employee Name: " + employeeName);
            System.out.println("✅ Extracted Course Name: " + courseName);
            System.out.println("🎫 Verification Code: " + certification.getVerificationCode());
            System.out.println("📅 Issue Date: " + certification.getIssueDate());

            System.out.println("✅ Extracted - First Name: " + firstName + ", Last Name: " + lastName);


            // Prepare data for PDF with separate first/last names
            Map<String, Object> pdfData = pdfGenerator.prepareCertificateData(
                    firstName,
                    lastName,  // Pass lastName separately
                    courseName,
                    certification.getIssueDate().toString(),
                    certification.getVerificationCode()
            );

            // ✅ Ensure certificate storage directory exists
            Path certificatesDir = Paths.get(certificateStoragePath);
            System.out.println("📁 Certificate storage path: " + certificatesDir.toAbsolutePath());

            if (!Files.exists(certificatesDir)) {
                Files.createDirectories(certificatesDir);
                System.out.println("✅ Created certificates directory");
            }

            // Generate PDF
            String fileName = "certificate_" + certification.getId() + "_" +
                    System.currentTimeMillis() + ".pdf";
            Path filePath = certificatesDir.resolve(fileName);

            System.out.println("📄 Generating PDF at: " + filePath);
            pdfGenerator.generateCertificatePdf(pdfData, filePath.toString());

            // Update certificate path
            certification.setCertificatePath(filePath.toString());
            certificationRepository.save(certification);

            System.out.println("✅ PDF certificate generated successfully for: " + "First Name: " + firstName + ", Last Name: " + lastName);
            return new UrlResource(filePath.toUri());

        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Failed to generate PDF certificate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate PDF certificate", e);
        }
    }

    // Helper methods for better data extraction
  /*  private String extractEmployeeName(Map<String, Object> employeeInfo) {
        // Try different possible field names for employee name
        if (employeeInfo.get("fullName") != null) {
            return employeeInfo.get("fullName").toString();
        }
        if (employeeInfo.get("name") != null) {
            return employeeInfo.get("name").toString();
        }
        if (employeeInfo.get("firstName") != null && employeeInfo.get("lastName") != null) {
            return employeeInfo.get("firstName") + " " + employeeInfo.get("lastName");
        }
        if (employeeInfo.get("username") != null) {
            return employeeInfo.get("username").toString();
        }
        if (employeeInfo.get("employeeName") != null) {
            return employeeInfo.get("employeeName").toString();
        }
        return "Employee " + employeeInfo.getOrDefault("id", "Unknown");
    }*/
    // Enhanced employee name extraction
    private String extractEmployeeName(Map<String, Object> employeeInfo) {
        if (employeeInfo == null || employeeInfo.isEmpty()) {
            return "Employee " + System.currentTimeMillis();
        }

        // Try multiple possible field names for employee name
        String[] possibleNameFields = {"fullName", "name", "employeeName", "username", "firstName"};

        for (String field : possibleNameFields) {
            if (employeeInfo.get(field) != null) {
                String name = employeeInfo.get(field).toString();
                if (!name.trim().isEmpty() && !name.equals("null")) {
                    System.out.println("✅ Found employee name in field '" + field + "': " + name);
                    return name;
                }
            }
        }

        // If first and last name are separate
        if (employeeInfo.get("firstName") != null && employeeInfo.get("lastName") != null) {
            String firstName = employeeInfo.get("firstName").toString();
            String lastName = employeeInfo.get("lastName").toString();
            if (!firstName.equals("null") && !lastName.equals("null")) {
                String fullName = firstName + " " + lastName;
                System.out.println("✅ Constructed name from first/last: " + fullName);
                return fullName;
            }
        }

        // Final fallback
        String fallbackName = "Employee " + (employeeInfo.get("id") != null ?
                employeeInfo.get("id") : System.currentTimeMillis());
        System.out.println("⚠️ Using fallback name: " + fallbackName);
        return fallbackName;
    }

    private String extractEmployeeEmail(Map<String, Object> employeeInfo) {
        if (employeeInfo.get("email") != null) {
            return employeeInfo.get("email").toString();
        }
        if (employeeInfo.get("employeeEmail") != null) {
            return employeeInfo.get("employeeEmail").toString();
        }
        return "unknown@example.com";
    }

    private String extractCourseName(Map<String, Object> courseInfo) {
        if (courseInfo.get("title") != null) {
            return courseInfo.get("title").toString();
        }
        if (courseInfo.get("courseName") != null) {
            return courseInfo.get("courseName").toString();
        }
        if (courseInfo.get("name") != null) {
            return courseInfo.get("name").toString();
        }
        return "Course " + courseInfo.getOrDefault("id", "Unknown");
    }

    private String extractCourseDescription(Map<String, Object> courseInfo) {
        if (courseInfo.get("description") != null) {
            return courseInfo.get("description").toString();
        }
        if (courseInfo.get("courseDescription") != null) {
            return courseInfo.get("courseDescription").toString();
        }
        return "";
    }

    // Helper methods to call other services
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchEmployeeInfo(Long employeeId) {
        try {
            String url = userServiceUrl + "/api/users/" + employeeId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("data") instanceof Map) {
                return (Map<String, Object>) response.get("data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Map.of("employeeName", "Unknown", "employeeEmail", "unknown@example.com");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchCourseInfo(Long courseId) {
        try {
            String url = courseServiceUrl + "/api/courses/" + courseId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("data") instanceof Map) {
                return (Map<String, Object>) response.get("data");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Map.of("courseName", "Unknown Course", "description", "");
    }

    private String fetchUserRole(Long userId) {
        try {
            Map<String, Object> userInfo = fetchEmployeeInfo(userId);
            return userInfo.get("role") != null ? userInfo.get("role").toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getCourseProgress(Long employeeId, Long courseId) {
        try {
            String url = courseServiceUrl + "/api/courses/enrollments/" + employeeId;
            System.out.println("🔗 Calling course service: " + url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("📡 Course Service Response: " + response);

            if (response != null && response.get("data") instanceof List) {
                List<Map<String, Object>> enrollments = (List<Map<String, Object>>) response.get("data");
                System.out.println("📋 Found " + enrollments.size() + " enrollments");

                for (Map<String, Object> enrollment : enrollments) {
                    System.out.println("🔍 Checking enrollment: " + enrollment);

                    // Debug: Print all keys in the enrollment
                    System.out.println("🔑 Enrollment keys: " + enrollment.keySet());

                    Object courseIdObj = enrollment.get("courseId");
                    System.out.println("🎯 Enrollment courseId: " + courseIdObj + " (type: " + (courseIdObj != null ? courseIdObj.getClass().getSimpleName() : "null") + ")");

                    if (courseIdObj != null) {
                        Long enrollmentCourseId = ((Number) courseIdObj).longValue();
                        System.out.println("🎯 Converted courseId: " + enrollmentCourseId + " | Looking for: " + courseId);

                        if (enrollmentCourseId.equals(courseId)) {
                            Object progressObj = enrollment.get("progress");
                            System.out.println("✅ FOUND MATCH! Progress object: " + progressObj + " (type: " + (progressObj != null ? progressObj.getClass().getSimpleName() : "null") + ")");

                            if (progressObj instanceof Number) {
                                int progress = ((Number) progressObj).intValue();
                                System.out.println("🎯 EXTRACTED PROGRESS: " + progress + "%");
                                return progress;
                            } else {
                                System.out.println("❌ Progress is not a number: " + progressObj);
                            }
                        }
                    }
                }
                System.out.println("❌ No matching enrollment found for course: " + courseId);
            } else {
                System.out.println("❌ Invalid response format or no data");
            }

            return 0;

        } catch (Exception e) {
            System.err.println("💥 ERROR getting course progress: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}