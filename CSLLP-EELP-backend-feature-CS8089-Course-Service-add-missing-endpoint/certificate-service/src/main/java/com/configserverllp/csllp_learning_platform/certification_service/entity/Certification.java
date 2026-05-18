package com.configserverllp.csllp_learning_platform.certification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Column(nullable = false)
    private String status; // ACTIVE, EXPIRED, REVOKED

    @Column(unique = true)
    private String verificationCode;

    private String certificatePath; // PDF file path

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Pre-persist to generate verification code
    @PrePersist
    public void generateVerificationCode() {
        if (this.verificationCode == null) {
            this.verificationCode = "CERT-" + System.currentTimeMillis() + "-" + this.employeeId;
        }
        if (this.issueDate == null) {
            this.issueDate = LocalDate.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }
}