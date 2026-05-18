package com.configserverllp.csllp_learning_platform.exam_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id","employee_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(length = 4000)
    private String answersJson; // raw answers JSON as stored

    private Double score; //

    @Column(nullable = false)
    private String status; // IN_PROGRESS, COMPLETED, REVIEW_PENDING

    @Column(length = 2000)
    private String feedback; // Added for employee feedback

    @CreationTimestamp
    private LocalDateTime startedAt;

    @UpdateTimestamp
    private LocalDateTime submittedAt;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    private String certificateUrl;

}
