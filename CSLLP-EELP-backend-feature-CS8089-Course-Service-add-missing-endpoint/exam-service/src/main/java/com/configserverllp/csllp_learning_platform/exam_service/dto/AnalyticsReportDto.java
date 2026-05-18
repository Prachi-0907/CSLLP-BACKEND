package com.configserverllp.csllp_learning_platform.exam_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportDto {
    private Long employeeId;
    private Long examId;
    private Double averageMarks;
    private Integer attempts;
}
