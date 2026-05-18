package com.configserverllp.csllp_learning_platform.user_service.controller;

import com.configserverllp.csllp_learning_platform.user_service.dto.*;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import com.configserverllp.csllp_learning_platform.user_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =====================================
    // CRUD Endpoints
    // =====================================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        UserResponse resp = mapToResponse(userService.getUserById(id));
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", resp));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest updateReq) {
        UserResponse updated = mapToResponse(userService.updateUser(id, updateReq));
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User soft-deleted", null));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        User activated = userService.activateUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User activated", mapToResponse(activated)));
    }


//    @GetMapping("/{id}/exists")
//    public ResponseEntity<Boolean> checkUserExists(@PathVariable Long id) {
//        boolean exists = userService.existsById(id);
//        return ResponseEntity.ok(exists);
//    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(value = "managerId", required = false) Long managerId) {
        List<User> users = (managerId == null) ? userService.getUsersByRole("ALL") : userService.getUsersByManagerId(managerId);
        List<UserResponse> resp = users.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", resp));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable String role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserResponse> resp = users.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", resp));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> checkUserExists(@PathVariable Long id) {
        boolean exists = userService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    // =====================================
    // Profile Endpoints
    // =====================================
    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable Long id) {
        UserResponse profile = userService.getProfile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile fetched", profile));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @RequestBody UserRequest updateReq) {
        UserResponse updatedProfile = userService.updateProfile(id, updateReq);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated", updatedProfile));
    }

    // =====================================
    // Dashboard Endpoint
    // =====================================
    @GetMapping("/dashboard/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getDashboard(@PathVariable Long id) {
        // Fetch user profile first
        UserResponse dashboard = userService.getProfile(id);

        // If admin or manager, fill dashboard metrics
        if ("ADMIN".equalsIgnoreCase(dashboard.getRole()) || "MANAGER".equalsIgnoreCase(dashboard.getRole())) {
            dashboard.setTotalEmployees(userService.getTotalEmployees());
            dashboard.setTotalCourses(userService.getTotalCourses());
            dashboard.setCompletedCourses(userService.getCompletedCourses());
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard data fetched", dashboard));
    }


    // =====================================
    // Search Endpoint
    // =====================================
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam String query) {
        List<UserResponse> resp = userService.searchUsers(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Search results", resp));
    }

    // =====================================
    // Utility: Map User to UserResponse
    // =====================================
    // ========== NEW ENDPOINTS FOR REPORTING ==========

    // Get employees under a specific manager
    @GetMapping("/manager/{managerId}/team")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getManagerTeam(@PathVariable Long managerId) {
        List<User> teamMembers = userService.getUsersByManagerId(managerId);
        List<UserResponse> resp = teamMembers.stream()
                .filter(user -> "EMPLOYEE".equals(user.getRole())) // Only return EMPLOYEES, not other managers
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Manager team fetched", resp));
    }

    // Get employee count for admin dashboard
    @GetMapping("/count/employees")
    public ResponseEntity<ApiResponse<Long>> getEmployeeCount() {
        Long count = userService.getUserCountByRole("EMPLOYEE");
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee count fetched", count));
    }

    // Get user count by any role
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserCountByRole(@RequestParam String role) {
        Long count = userService.getUserCountByRole(role);
        return ResponseEntity.ok(new ApiResponse<>(true, role + " count fetched", count));
    }


    private UserResponse mapToResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .managerId(u.getManagerId())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .profilePhotoUrl(u.getProfilePhotoUrl())
                .build();
    }

//    // ADD SEARCH ENDPOINT:
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchUsers(
//            @RequestParam String query,
//            @RequestParam(required = false) String role) {
//        // Search by name, email, or employee ID
//        // Return: ID, name, email, department, employeeId
//    }

}
