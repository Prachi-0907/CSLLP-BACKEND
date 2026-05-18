package com.configserverllp.csllp_learning_platform.user_service.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @NotBlank(message = "Role is required")
    private String role; // ADMIN, MANAGER, EMPLOYEE, HR

    // optional: for admin-created employee
    private Long managerId;

    //@NotBlank(message = "Password is required")
    //@Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String profilePhotoUrl; // NEW
}

