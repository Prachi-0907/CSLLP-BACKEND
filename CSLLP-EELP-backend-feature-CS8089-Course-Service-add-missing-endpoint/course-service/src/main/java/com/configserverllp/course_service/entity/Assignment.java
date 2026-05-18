package com.configserverllp.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long courseId;

    private Long employeeId;

    // Priority could be LOW/MEDIUM/HIGH - store as string for simplicity
    //private String priority;

    private LocalDate dueDate;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, APPROVED, REJECTED, IN_PROGRESS, COMPLETED

    private Long assignedBy;

    private Long approvedBy;
    private LocalDateTime approvedAt;

    private Long rejectedBy;
    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, APPROVED, REJECTED, IN_PROGRESS, COMPLETED
    }
}
