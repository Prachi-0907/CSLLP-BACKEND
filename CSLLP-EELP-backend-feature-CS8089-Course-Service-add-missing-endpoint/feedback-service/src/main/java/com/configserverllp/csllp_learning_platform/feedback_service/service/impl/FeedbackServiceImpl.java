package com.configserverllp.csllp_learning_platform.feedback_service.service.impl;

import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackRequest;
import com.configserverllp.csllp_learning_platform.feedback_service.dto.FeedbackResponse;
import com.configserverllp.csllp_learning_platform.feedback_service.entity.Feedback;
import com.configserverllp.csllp_learning_platform.feedback_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.feedback_service.repository.FeedbackRepository;
import com.configserverllp.csllp_learning_platform.feedback_service.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository repo;
    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceBaseUrl;

    @Value("${course.service.url}")
    private String courseServiceBaseUrl;

    @Value("${exam.service.url}")
    private String examServiceBaseUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Override
    public FeedbackResponse submitFeedback(FeedbackRequest req) {
        // Validate that for USER type, targetId and receivedByUserId are the same
        if ("USER".equalsIgnoreCase(req.getTargetType())) {
            req.setReceivedByUserId(req.getTargetId());
        }

        Feedback f = Feedback.builder()
                .userId(req.getUserId())
                .targetType(req.getTargetType().toUpperCase())
                .targetId(req.getTargetId())
                .receivedByUserId(req.getReceivedByUserId())
                .rating(req.getRating())
                .comments(req.getComments())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        Feedback saved = repo.save(f);

        // Enhanced notifications for user feedbacks
        try {
            Long managerId = fetchManagerIdForUser(req.getUserId());

            Map<String, Object> payloadAdmin = Map.of(
                    "receiverRole", "ADMIN",
                    "message", "New " + saved.getTargetType() + " feedback submitted by userId: " + saved.getUserId(),
                    "timestamp", LocalDateTime.now().toString()
            );
            restTemplate.postForObject(notificationServiceUrl, payloadAdmin, Object.class);

            // Notify manager
            if (managerId != null) {
                Map<String, Object> payloadMgr = Map.of(
                        "receiverRole", "MANAGER",
                        "receiverId", String.valueOf(managerId),
                        "message", "New " + saved.getTargetType() + " feedback from your team member userId: " + saved.getUserId(),
                        "timestamp", LocalDateTime.now().toString()
                );
                restTemplate.postForObject(notificationServiceUrl, payloadMgr, Object.class);
            }

            // NEW: Notify the user who received the feedback (if it's user feedback)
            if ("USER".equalsIgnoreCase(saved.getTargetType()) && saved.getReceivedByUserId() != null) {
                Map<String, Object> payloadReceiver = Map.of(
                        "receiverId", String.valueOf(saved.getReceivedByUserId()),
                        "receiverRole", "EMPLOYEE",
                        "message", "You received new feedback from " + fetchUserName(saved.getUserId()),
                        "timestamp", LocalDateTime.now().toString()
                );
                restTemplate.postForObject(notificationServiceUrl, payloadReceiver, Object.class);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return toResponse(saved);
    }

    @Override
    public List<FeedbackResponse> getMyFeedbacks(Long userId) {
        return repo.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // NEW: Get feedbacks received by user
    @Override
    public List<FeedbackResponse> getReceivedFeedbacks(Long userId) {
        return repo.findFeedbacksReceivedByUser(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // NEW: Get feedbacks given by user (alias for getMyFeedbacks)
    @Override
    public List<FeedbackResponse> getGivenFeedbacks(Long userId) {
        return getMyFeedbacks(userId);
    }

    @Override
    public List<FeedbackResponse> getFeedbacksForTarget(String targetType, Long targetId) {
        return repo.findByTargetTypeAndTargetId(targetType.toUpperCase(), targetId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTeamFeedbackSummary(Long managerId) {
        String url = userServiceBaseUrl + "/api/users/manager/" + managerId + "/team";
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> data = resp == null ? Collections.emptyList() : (List<Map<String, Object>>) resp.get("data");
        List<Long> teamIds = data.stream().map(m -> Long.parseLong(m.get("id").toString())).collect(Collectors.toList());

        List<Feedback> teamFeedbacks = teamIds.isEmpty() ? Collections.emptyList() : repo.findByUserIdIn(teamIds);
        double avg = teamFeedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("teamFeedbacks", teamFeedbacks.stream().map(this::toResponse).collect(Collectors.toList()));
        result.put("averageRating", avg);
        result.put("totalFeedbacks", teamFeedbacks.size());
        return result;
    }

    @Override
    public List<FeedbackResponse> getAllFeedbacks() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public void flagFeedback(Long id) {
        Feedback f = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        f.setStatus("FLAGGED");
        repo.save(f);

        try {
            Map<String, Object> payload = Map.of(
                    "receiverId", String.valueOf(f.getUserId()),
                    "receiverRole", "EMPLOYEE",
                    "message", "Your feedback (id: " + f.getId() + ") was flagged by Admin",
                    "timestamp", LocalDateTime.now().toString()
            );
            restTemplate.postForObject(notificationServiceUrl, payload, Object.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Double getAverageRating(String targetType, Long targetId) {
        List<Feedback> list = repo.findByTargetTypeAndTargetId(targetType.toUpperCase(), targetId);
        return list.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
    }

    // NEW: Search methods
    @Override
    public List<FeedbackResponse> searchReceivedFeedbacks(Long userId, String category, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Feedback> feedbacks = repo.searchReceivedFeedbacks(userId, category, fromDate, toDate);
        return feedbacks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<FeedbackResponse> searchGivenFeedbacks(Long userId, String category, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Feedback> feedbacks = repo.searchGivenFeedbacks(userId, category, fromDate, toDate);
        return feedbacks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // NEW: Stats method
    @Override
    public Map<String, Object> getFeedbackStats(Long userId) {
        List<Feedback> received = repo.findFeedbacksReceivedByUser(userId);
        List<Feedback> given = repo.findByUserId(userId);

        double avgRating = received.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        long pendingCount = received.stream()
                .filter(f -> f.getStatus().equals("ACTIVE"))
                .count();

        long resolvedCount = received.stream()
                .filter(f -> f.getStatus().equals("FLAGGED"))
                .count();

        // Category distribution (simplified - you might want to extract categories from comments)
        Map<String, Long> categoryDistribution = new HashMap<>();
        received.forEach(f -> {
            String category = extractCategoryFromComments(f.getComments());
            categoryDistribution.put(category, categoryDistribution.getOrDefault(category, 0L) + 1);
        });

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedbacks", received.size());
        stats.put("averageRating", avgRating);
        stats.put("pendingCount", pendingCount);
        stats.put("resolvedCount", resolvedCount);
        stats.put("givenCount", given.size());
        stats.put("categoryDistribution", categoryDistribution);

        return stats;
    }

    private String extractCategoryFromComments(String comments) {
        if (comments == null) return "GENERAL";

        String commentLower = comments.toLowerCase();
        if (commentLower.contains("technical") || commentLower.contains("code") || commentLower.contains("programming")) {
            return "TECHNICAL";
        } else if (commentLower.contains("communication") || commentLower.contains("teamwork") || commentLower.contains("collaboration")) {
            return "COMMUNICATION";
        } else if (commentLower.contains("leadership") || commentLower.contains("management")) {
            return "LEADERSHIP";
        } else {
            return "GENERAL";
        }
    }

    // Enhanced helper method to include received user info
    private FeedbackResponse toResponse(Feedback f) {
        FeedbackResponse.FeedbackResponseBuilder builder = FeedbackResponse.builder()
                .id(f.getId())
                .userId(f.getUserId())
                .userName(fetchUserName(f.getUserId()))
                .targetType(f.getTargetType())
                .targetId(f.getTargetId())
                .targetTitle(fetchTargetTitle(f.getTargetType(), f.getTargetId()))
                .rating(f.getRating())
                .comments(f.getComments())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt());

        // NEW: Add received user info for user feedbacks
        if ("USER".equalsIgnoreCase(f.getTargetType()) && f.getReceivedByUserId() != null) {
            builder.receivedByUserId(f.getReceivedByUserId())
                    .receivedByUserName(fetchUserName(f.getReceivedByUserId()));
        } else if (f.getReceivedByUserId() != null) {
            builder.receivedByUserId(f.getReceivedByUserId())
                    .receivedByUserName(fetchUserName(f.getReceivedByUserId()));
        }

        return builder.build();
    }

    // Enhanced target title fetcher for USER type
    private String fetchTargetTitle(String targetType, Long targetId) {
        try {
            if ("COURSE".equalsIgnoreCase(targetType)) {
                String url = courseServiceBaseUrl + "/courses/" + targetId;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                if (resp != null && resp.get("data") instanceof Map) {
                    Map<?, ?> data = (Map<?, ?>) resp.get("data");
                    Object title = data.get("title");
                    return title == null ? null : title.toString();
                }
            } else if ("EXAM".equalsIgnoreCase(targetType)) {
                String url = examServiceBaseUrl + "/api/exams/" + targetId;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                if (resp != null && resp.get("data") instanceof Map) {
                    Map<?, ?> data = (Map<?, ?>) resp.get("data");
                    Object title = data.get("title");
                    return title == null ? null : title.toString();
                }
            } else if ("USER".equalsIgnoreCase(targetType)) {
                // For user feedbacks, return the user's name as target title
                String userName = fetchUserName(targetId);
                return userName != null ? userName : "User " + targetId;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String fetchUserName(Long userId) {
        try {
            String url = userServiceBaseUrl + "/api/users/" + userId;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) resp.get("data");
                Object fn = data.get("firstName");
                Object ln = data.get("lastName");
                if (fn != null || ln != null) {
                    return String.format("%s %s", fn == null ? "" : fn, ln == null ? "" : ln).trim();
                }
                Object email = data.get("email");
                return email == null ? null : email.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Long fetchManagerIdForUser(Long userId) {
        try {
            String url = userServiceBaseUrl + "/api/users/" + userId;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) resp.get("data");
                Object managerId = data.get("managerId");
                if (managerId != null) return Long.parseLong(managerId.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    // NEW: Admin statistics method
    @Override
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get all feedbacks
            List<Feedback> allFeedbacks = repo.findAll();

            // Count feedbacks by type
            long courseFeedbacksCount = allFeedbacks.stream()
                    .filter(f -> "COURSE".equalsIgnoreCase(f.getTargetType()))
                    .count();

            long examFeedbacksCount = allFeedbacks.stream()
                    .filter(f -> "EXAM".equalsIgnoreCase(f.getTargetType()))
                    .count();

            // Calculate overall average rating
            double overallAvgRating = allFeedbacks.stream()
                    .mapToInt(Feedback::getRating)
                    .average()
                    .orElse(0.0);

            // Prepare statistics
            stats.put("totalFeedbacks", allFeedbacks.size());
            stats.put("courseFeedbacksCount", courseFeedbacksCount);
            stats.put("examFeedbacksCount", examFeedbacksCount);
            stats.put("overallAvgRating", Math.round(overallAvgRating * 100.0) / 100.0);

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values in case of errors
            stats.put("totalFeedbacks", 0);
            stats.put("courseFeedbacksCount", 0);
            stats.put("examFeedbacksCount", 0);
            stats.put("overallAvgRating", 0.0);
        }

        return stats;
    }
}