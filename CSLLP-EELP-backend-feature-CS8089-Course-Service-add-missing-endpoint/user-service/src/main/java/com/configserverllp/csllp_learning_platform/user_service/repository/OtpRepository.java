package com.configserverllp.csllp_learning_platform.user_service.repository;

import com.configserverllp.csllp_learning_platform.user_service.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByEmailAndOtpCodeAndUsedFalse(String email, String otpCode);

    List<Otp> findByEmailAndUsedFalse(String email);

    @Modifying
    @Query("UPDATE Otp o SET o.used = true WHERE o.email = :email AND o.used = false")
    void invalidatePreviousOtps(@Param("email") String email);

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt > :since AND o.used = false")
    long countRecentOtps(@Param("email") String email, @Param("since") LocalDateTime since);
}