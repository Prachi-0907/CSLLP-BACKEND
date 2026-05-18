package com.configserverllp.csllp_learning_platform.feedback_service.repository;

import com.configserverllp.csllp_learning_platform.feedback_service.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByUserId(Long userId); // Given feedbacks
    List<Feedback> findByTargetTypeAndTargetId(String targetType, Long targetId);
    List<Feedback> findByUserIdIn(List<Long> userIds);

    // NEW: For received feedbacks (when user is the target)
    List<Feedback> findByTargetTypeAndReceivedByUserId(String targetType, Long receivedByUserId);
    List<Feedback> findByReceivedByUserId(Long receivedByUserId);

    // NEW: Combined query for all feedbacks received by user (both as target and through receivedByUserId)
    @Query("SELECT f FROM Feedback f WHERE (f.targetType = 'USER' AND f.targetId = :userId) OR f.receivedByUserId = :userId")
    List<Feedback> findFeedbacksReceivedByUser(@Param("userId") Long userId);

    // NEW: Search methods for received feedbacks
    @Query("SELECT f FROM Feedback f WHERE ((f.targetType = 'USER' AND f.targetId = :userId) OR f.receivedByUserId = :userId) " +
            "AND (:category IS NULL OR f.comments LIKE %:category%) " +
            "AND (:fromDate IS NULL OR f.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR f.createdAt <= :toDate)")
    List<Feedback> searchReceivedFeedbacks(@Param("userId") Long userId,
                                           @Param("category") String category,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate);

    // NEW: Search methods for given feedbacks
    @Query("SELECT f FROM Feedback f WHERE f.userId = :userId " +
            "AND (:category IS NULL OR f.comments LIKE %:category%) " +
            "AND (:fromDate IS NULL OR f.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR f.createdAt <= :toDate)")
    List<Feedback> searchGivenFeedbacks(@Param("userId") Long userId,
                                        @Param("category") String category,
                                        @Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate);
}