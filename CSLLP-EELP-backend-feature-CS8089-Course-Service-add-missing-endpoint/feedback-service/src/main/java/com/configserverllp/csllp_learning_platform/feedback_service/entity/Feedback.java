package com.configserverllp.csllp_learning_platform.feedback_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who submitted feedback
    private Long userId;

    // "COURSE", "EXAM", or "USER"
    private String targetType;

    // courseId, examId, or userId (if targetType is USER)
    private Long targetId;

    // NEW: For user feedbacks - who received the feedback (only when targetType is USER)
    private Long receivedByUserId;

    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comments;

    private String status; // ACTIVE / FLAGGED

    private LocalDateTime createdAt;
}