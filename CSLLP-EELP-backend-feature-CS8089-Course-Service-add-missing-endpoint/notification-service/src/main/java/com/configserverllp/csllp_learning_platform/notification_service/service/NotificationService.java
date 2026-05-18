package com.configserverllp.csllp_learning_platform.notification_service.service;

import com.configserverllp.csllp_learning_platform.notification_service.dto.EmailRequest;

public interface NotificationService {
    void sendEmail(EmailRequest request);
}
