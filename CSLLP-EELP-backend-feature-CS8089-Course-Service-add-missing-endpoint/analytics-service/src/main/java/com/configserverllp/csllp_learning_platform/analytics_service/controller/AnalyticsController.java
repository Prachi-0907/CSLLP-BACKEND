package com.configserverllp.csllp_learning_platform.analytics_service.controller;

import com.configserverllp.csllp_learning_platform.analytics_service.dto.AnalyticsReportDto;
import com.configserverllp.csllp_learning_platform.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping
    public ResponseEntity<AnalyticsReportDto> createReport(@RequestBody AnalyticsReportDto dto) {
        return ResponseEntity.ok(analyticsService.createReport(dto));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AnalyticsReportDto>> getReportsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(analyticsService.getReportsByEmployee(employeeId));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<AnalyticsReportDto>> getReportsByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(analyticsService.getReportsByExam(examId));
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsReportDto>> getAllReports() {
        return ResponseEntity.ok(analyticsService.getAllReports());
    }
}
