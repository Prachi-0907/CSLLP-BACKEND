package com.configserverllp.csllp_learning_platform.analytics_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analytics_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private Long examId;
    private Double averageMarks;
    private Integer attempts;
}
