package com.cms.demo.service;

import com.cms.demo.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messaging;

    /**
     * employeeCode = login username (e.g. 25-00001)
     * Sends ONCE via topic only (no duplicate).
     */
    public void pushToEmployee(String employeeCode, NotificationDto dto) {
        if (employeeCode == null || employeeCode.isBlank() || dto == null) {
            return;
        }
        String user = employeeCode.trim();
        System.out.println("[WS] push topic → /topic/notifications." + user);
        try {
            messaging.convertAndSend("/topic/notifications." + user, dto);
        } catch (Exception e) {
            System.err.println("[WS] push failed: " + e.getMessage());
        }
    }
}