package com.kanban.controller;

import com.kanban.model.Notification;
import com.kanban.model.User;
import com.kanban.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Get all notifications (Admin sees all, User sees only their notifications)
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getNotifications(user);

        // Debug logging
        System.out.println("=== NOTIFICATION DEBUG ===");
        System.out.println("User: " + user.getUsername() + " | Role: " + user.getRole());
        System.out.println("Total notifications: " + notifications.size());

        // Group by task ID and type to see duplicates
        Map<String, Long> duplicateCheck = notifications.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getTaskId() + "_" + n.getType(),
                        Collectors.counting()
                ));

        System.out.println("Notification groups:");
        duplicateCheck.forEach((key, count) -> {
            if (count > 1) {
                System.out.println("  DUPLICATE: " + key + " appears " + count + " times");
            } else {
                System.out.println("  OK: " + key);
            }
        });

        // Show detailed info for each notification
        notifications.forEach(n -> {
            System.out.println("  ID: " + n.getId() +
                    " | Target: " + n.getTargetUser() +
                    " | TriggeredBy: " + n.getTriggeredBy() +
                    " | Type: " + n.getType() +
                    " | TaskId: " + n.getTaskId() +
                    " | Read: " + n.isRead());
        });

        System.out.println("=== END DEBUG ===");

        return ResponseEntity.ok(notifications);
    }

    // Get unread count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // Mark notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    // NEW: Mark all notifications as read for the current user
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }
}