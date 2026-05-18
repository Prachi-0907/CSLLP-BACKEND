package com.configserverllp.csllp_learning_platform.notification_service.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String to;
    private String subject;
    private String message;
}
