package com.configserverllp.csllp_learning_platform.user_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchResponse {
    private Long id;
    private String employeeId;
    private String name;
    private String email;
    private String role;
    private String department;
    private String status;
}