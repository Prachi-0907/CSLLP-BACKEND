package com.configserverllp.csllp_learning_platform.analytics_service.repository;

import com.configserverllp.csllp_learning_platform.analytics_service.entity.AnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, Long> {
    List<AnalyticsReport> findByEmployeeId(Long employeeId);
    List<AnalyticsReport> findByExamId(Long examId);
}
