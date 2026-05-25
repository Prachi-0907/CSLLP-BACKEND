package com.configserverllp.course_service.scheduler;

import com.configserverllp.course_service.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private final CourseService courseService;

    // Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    // Runs every minute
//    @Scheduled(cron = "0 * * * * ?")
    public void runDailyReminders() {

        System.out.println("🔄 Running scheduled reminders...");

        courseService.checkAndSendProgressReminders();
    }
}
