package com.configserverllp.csllp_learning_platform.user_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long id;
    private String email;
    private String role;
    private Long managerId;

    // New fields
    private UserResponse profile;           // User profile details
    private UserResponse dashboard;
}
