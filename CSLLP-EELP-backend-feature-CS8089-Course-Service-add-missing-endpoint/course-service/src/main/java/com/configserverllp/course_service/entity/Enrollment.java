package com.configserverllp.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long courseId;

    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    private int progress;

    private LocalDateTime enrolledAt;

    // --- New fields to track approvals/rejections ---
    private Long approvedBy;
    private LocalDateTime approvedAt;

    private Long rejectedBy;
    private LocalDateTime rejectedAt;

    private LocalDate dueDate;

    // ✅ NEW FIELD
    private LocalDateTime lastReminderSentAt;

    public enum Status {
        PENDING_APPROVAL, APPROVED, REJECTED, IN_PROGRESS, COMPLETED
    }
}
