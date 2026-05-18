package com.configserverllp.csllp_learning_platform.submission_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long examId;     // Reference to exam
    private Long userId;     // Reference to user
    private String answer;   // Could be JSON string for MCQ/coding

    private Double marks;    // Nullable until evaluated

    private LocalDateTime submittedAt;
}
