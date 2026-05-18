package com.configserverllp.csllp_learning_platform.user_service.service;

import com.configserverllp.csllp_learning_platform.user_service.dto.LoginRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserResponse;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserSearchResponse;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;

import java.util.List;

public interface UserService {
    User createUser(UserRequest req, Long creatorId, String creatorRole);
    User getUserById(Long id);
    User updateUser(Long id, UserRequest update);
    void softDeleteUser(Long id);
    List<User> getUsersByManagerId(Long managerId);
    List<User> getUsersByRole(String role);
    User login(LoginRequest req);
    boolean existsById(Long id);
    User activateUser(Long id);
    // NEW METHOD
    Long getUserCountByRole(String role);


    // New methods for profile & dashboard
    UserResponse getProfile(Long userId);
    UserResponse updateProfile(Long userId, UserRequest updateReq);
    int getTotalEmployees();
    int getTotalCourses();
    int getCompletedCourses();
    List<User> searchUsers(String query);

    // Add these methods to UserService interface
    String generateOtp(String email);
    boolean verifyOtp(String email, String otp);
    boolean resetPassword(String email, String otp, String newPassword);

    List<UserSearchResponse> searchUsersForAssignments(String query, String role);
}
