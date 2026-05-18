package com.configserverllp.csllp_learning_platform.notification_service.controller;

import com.configserverllp.csllp_learning_platform.notification_service.dto.EmailRequest;
import com.configserverllp.csllp_learning_platform.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send-email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        notificationService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully to " + request.getTo());
    }
}
