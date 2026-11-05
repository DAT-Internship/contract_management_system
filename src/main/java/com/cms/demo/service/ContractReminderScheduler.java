package com.cms.demo.service;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContractReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ContractReminderScheduler.class);

    private final ContractReminderService reminderService;

    public ContractReminderScheduler(ContractReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${app.reminders.cron:0 10 0 * * *}")
    public void sendDailyReminders() {
        int sent = reminderService.sendScheduledReminders(LocalDate.now());
        if (sent > 0) {
            log.info("Daily contract reminders created for {} recipient(s)", sent);
        }
    }
}
