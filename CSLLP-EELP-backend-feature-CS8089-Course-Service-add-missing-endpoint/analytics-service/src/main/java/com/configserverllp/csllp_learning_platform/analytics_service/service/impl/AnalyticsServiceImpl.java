package com.configserverllp.csllp_learning_platform.analytics_service.service.impl;

import com.configserverllp.csllp_learning_platform.analytics_service.dto.AnalyticsReportDto;
import com.configserverllp.csllp_learning_platform.analytics_service.entity.AnalyticsReport;
import com.configserverllp.csllp_learning_platform.analytics_service.repository.AnalyticsReportRepository;
import com.configserverllp.csllp_learning_platform.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsReportRepository repository;

    private AnalyticsReportDto mapToDto(AnalyticsReport report) {
        return AnalyticsReportDto.builder()
                .id(report.getId())
                .employeeId(report.getEmployeeId())
                .examId(report.getExamId())
                .averageMarks(report.getAverageMarks())
                .attempts(report.getAttempts())
                .build();
    }

    private AnalyticsReport mapToEntity(AnalyticsReportDto dto) {
        return AnalyticsReport.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .examId(dto.getExamId())
                .averageMarks(dto.getAverageMarks())
                .attempts(dto.getAttempts())
                .build();
    }

    @Override
    public AnalyticsReportDto createReport(AnalyticsReportDto dto) {
        AnalyticsReport report = repository.save(mapToEntity(dto));
        return mapToDto(report);
    }

    @Override
    public List<AnalyticsReportDto> getReportsByEmployee(Long employeeId) {
        return repository.findByEmployeeId(employeeId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportDto> getReportsByExam(Long examId) {
        return repository.findByExamId(examId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportDto> getAllReports() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }
}
