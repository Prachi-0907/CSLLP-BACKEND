package com.configserverllp.csllp_learning_platform.feedback_service.service;

import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackRequest;
import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FeedbackService {
    FeedbackResponse submitFeedback(FeedbackRequest req);
    List<FeedbackResponse> getMyFeedbacks(Long userId); // Given feedbacks
    List<FeedbackResponse> getReceivedFeedbacks(Long userId); // NEW: Received feedbacks
    List<FeedbackResponse> getGivenFeedbacks(Long userId); // NEW: Given feedbacks (alias for getMyFeedbacks)
    List<FeedbackResponse> getFeedbacksForTarget(String targetType, Long targetId);
    Map<String, Object> getTeamFeedbackSummary(Long managerId);
    List<FeedbackResponse> getAllFeedbacks();
    void flagFeedback(Long id);
    Double getAverageRating(String targetType, Long targetId);

    // NEW: Search methods
    List<FeedbackResponse> searchReceivedFeedbacks(Long userId, String category, LocalDateTime fromDate, LocalDateTime toDate);
    List<FeedbackResponse> searchGivenFeedbacks(Long userId, String category, LocalDateTime fromDate, LocalDateTime toDate);

    // NEW: Stats methods
    Map<String, Object> getFeedbackStats(Long userId);
    // NEW: Admin statistics
    Map<String, Object> getAdminStats();
}