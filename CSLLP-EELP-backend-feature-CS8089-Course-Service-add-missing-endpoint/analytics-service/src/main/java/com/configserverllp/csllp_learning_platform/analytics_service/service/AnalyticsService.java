package com.configserverllp.csllp_learning_platform.analytics_service.service;

import com.configserverllp.csllp_learning_platform.analytics_service.dto.AnalyticsReportDto;

import java.util.List;

public interface AnalyticsService {
    AnalyticsReportDto createReport(AnalyticsReportDto dto);
    List<AnalyticsReportDto> getReportsByEmployee(Long employeeId);
    List<AnalyticsReportDto> getReportsByExam(Long examId);
    List<AnalyticsReportDto> getAllReports();
}
