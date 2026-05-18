package com.configserverllp.csllp_learning_platform.user_service.controller;

import com.configserverllp.csllp_learning_platform.user_service.dto.*;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import com.configserverllp.csllp_learning_platform.user_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.configserverllp.csllp_learning_platform.user_service.dto.ForgotPasswordRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.ResetPasswordRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.VerifyOtpRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRequest req,
            @RequestHeader(value = "X-Creator-Id", required = false) Long creatorId,
            @RequestHeader(value = "X-Creator-Role", required = false) String creatorRole) {

        String effectiveCreatorRole = creatorRole == null ? "ADMIN" : creatorRole;
        User saved = userService.createUser(req, creatorId, effectiveCreatorRole);

        // Return UserResponse directly
        UserResponse profile = userService.getProfile(saved.getId());

        return ResponseEntity.status(201).body(new ApiResponse<>(true, "User created", profile));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest req) {
        User u = userService.login(req);

        // Get UserResponse (profile + dashboard)
        UserResponse profile = userService.getProfile(u.getId());

        if ("ADMIN".equalsIgnoreCase(u.getRole()) || "MANAGER".equalsIgnoreCase(u.getRole())) {
            profile.setTotalEmployees(userService.getTotalEmployees());
            profile.setTotalCourses(userService.getTotalCourses());
            profile.setCompletedCourses(userService.getCompletedCourses());
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", profile));
    }

// Add these endpoints to AuthController class

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String otp = userService.generateOtp(request.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent to your email", "OTP sent successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = userService.verifyOtp(request.getEmail(), request.getOtp());
        if (isValid) {
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP verified successfully", "OTP is valid"));
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Invalid OTP", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = userService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successfully", "Password updated"));
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to reset password", null));
    }
}
