package com.configserverllp.csllp_learning_platform.feedback_service.controller;

import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackRequest;
import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackResponse;
import com.configserverllp.csllp_learning_platform.feedback_service.service.FeedbackService;
import com.configserverllp.csllp_learning_platform.feedback_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FeedbackController {

    private final FeedbackService service;

    // Employee -> submit
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(@Valid @RequestBody FeedbackRequest req) {
        FeedbackResponse resp = service.submitFeedback(req);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feedback submitted", resp));
    }

    // Employee -> view own given feedbacks
    @GetMapping("/my/{userId}")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> myFeedbacks(@PathVariable Long userId) {
        List<FeedbackResponse> list = service.getMyFeedbacks(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "My feedbacks fetched", list));
    }

    // NEW: Get received feedbacks
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> receivedFeedbacks(@RequestParam Long employeeId) {
        List<FeedbackResponse> list = service.getReceivedFeedbacks(employeeId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Received feedbacks fetched", list));
    }

    // NEW: Get given feedbacks
    @GetMapping("/given")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> givenFeedbacks(@RequestParam Long givenBy) {
        List<FeedbackResponse> list = service.getGivenFeedbacks(givenBy);
        return ResponseEntity.ok(new ApiResponse<>(true, "Given feedbacks fetched", list));
    }

    // Admin -> view all
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> all() {
        List<FeedbackResponse> list = service.getAllFeedbacks();
        return ResponseEntity.ok(new ApiResponse<>(true, "All feedbacks fetched", list));
    }

    // Manager -> team summary
    @GetMapping("/team/{managerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> team(@PathVariable Long managerId) {
        Map<String, Object> summary = service.getTeamFeedbackSummary(managerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Team feedback summary", summary));
    }

    // Feedbacks for a specific Course or Exam
    @GetMapping("/{type}/{targetId}")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> byTarget(@PathVariable String type, @PathVariable Long targetId) {
        List<FeedbackResponse> list = service.getFeedbacksForTarget(type, targetId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feedbacks fetched", list));
    }

    // Admin -> flag feedback
    @PutMapping("/{id}/flag")
    public ResponseEntity<ApiResponse<Void>> flag(@PathVariable Long id) {
        service.flagFeedback(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feedback flagged", null));
    }

    // Average rating
    @GetMapping("/average/{type}/{targetId}")
    public ResponseEntity<ApiResponse<Double>> average(@PathVariable String type, @PathVariable Long targetId) {
        Double avg = service.getAverageRating(type, targetId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Average rating fetched", avg));
    }

    // NEW: Search received feedbacks
    @GetMapping("/search/received")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> searchReceived(
            @RequestParam Long employeeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<FeedbackResponse> list = service.searchReceivedFeedbacks(employeeId, category, fromDateTime, toDateTime);
        return ResponseEntity.ok(new ApiResponse<>(true, "Received feedbacks searched", list));
    }

    // NEW: Search given feedbacks
    @GetMapping("/search/given")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> searchGiven(
            @RequestParam Long givenBy,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        List<FeedbackResponse> list = service.searchGivenFeedbacks(givenBy, category, fromDateTime, toDateTime);
        return ResponseEntity.ok(new ApiResponse<>(true, "Given feedbacks searched", list));
    }

    // NEW: Get feedback stats
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@RequestParam Long userId) {
        Map<String, Object> stats = service.getFeedbackStats(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feedback stats fetched", stats));
    }

    // Admin -> statistics
    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminStats() {
        Map<String, Object> stats = service.getAdminStats();
        return ResponseEntity.ok(new ApiResponse<>(true, "Admin statistics fetched", stats));
    }
}