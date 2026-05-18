package com.configserverllp.csllp_learning_platform.analytics_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsReportDto {
    private Long id;
    private Long employeeId;
    private Long examId;
    private Double averageMarks;
    private Integer attempts;
}
