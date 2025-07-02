package com.kanban.service;

import com.kanban.model.Board;
import com.kanban.model.Notification;
import com.kanban.model.Task;
import com.kanban.model.User;
import com.kanban.repository.BoardRepository;
import com.kanban.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    BoardRepository boardRepository;

    // Get notifications for user (Admin sees all, User sees only their notifications)
    public List<Notification> getNotifications(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return notificationRepository.findAll();
        }
        return notificationRepository.findByTargetUserOrderByCreatedAtDesc(user.getUsername());
    }

    // Get unread count - Each user gets their own count
    public long getUnreadCount(User user) {
        return notificationRepository.countByTargetUserAndIsRead(user.getUsername(), false);
    }

    // Mark notification as read - Only affects the specific notification
    public void markAsRead(String notificationId, User user) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent()) {
            Notification notif = notification.get();
            // Admin can mark any notification, users can only mark their own
            if ("ADMIN".equals(user.getRole()) || notif.getTargetUser().equals(user.getUsername())) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        }
    }

    // Mark all notifications as read for the current user only
    public void markAllAsRead(User user) {
        List<Notification> notifications;

        if ("ADMIN".equals(user.getRole())) {
            // Admin marks all system notifications as read
            notifications = notificationRepository.findAll();
        } else {
            // User marks only their notifications as read
            notifications = notificationRepository.findByTargetUserOrderByCreatedAtDesc(user.getUsername());
        }

        notifications.stream()
                .filter(n -> !n.isRead()) // Only unread notifications
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    // Helper method to determine if a user should receive notifications
    private boolean shouldNotifyUser(String targetUser, String triggeredByUser) {
        // Don't notify if the user is notifying themselves
        return !targetUser.equals(triggeredByUser);
    }

    // Create notification when board is created
    public void createBoardNotification(Board board, User triggeredBy) {
        // Only notify if USER creates board (notify ADMIN)
        // Don't notify if ADMIN creates board
        if ("USER".equals(triggeredBy.getRole())) {
            String message = String.format("New board '%s' created by %s", board.getName(), triggeredBy.getUsername());
            Notification adminNotification = new Notification(
                    message, "BOARD_CREATED", null, null,
                    board.getId(), board.getName(), "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when task is created
    public void createTaskNotification(Task task, User triggeredBy) {
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        String boardName = board.map(Board::getName).orElse("Unknown Board");

        if ("ADMIN".equals(triggeredBy.getRole())) {
            // ADMIN creates task - notify assigned USER only (if task is assigned to someone else)
            if (task.getAssignedTo() != null &&
                    shouldNotifyUser(task.getAssignedTo(), triggeredBy.getUsername()) &&
                    !"ADMIN".equals(task.getAssignedTo())) {

                String message = String.format("Task '%s' has been assigned to you in board '%s'", task.getTitle(), boardName);
                Notification userNotification = new Notification(
                        message, "TASK_ASSIGNED", task.getId(), task.getTitle(),
                        task.getBoardId(), boardName, task.getAssignedTo(), triggeredBy.getUsername()
                );
                safeSaveNotification(userNotification);
            }
        } else {
            // USER creates task - notify ADMIN only
            String message = String.format("New task '%s' created by %s in board '%s'", task.getTitle(), triggeredBy.getUsername(), boardName);
            Notification adminNotification = new Notification(
                    message, "TASK_CREATED", task.getId(), task.getTitle(),
                    task.getBoardId(), boardName, "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when task is updated
    public void updateTaskNotification(Task task, User triggeredBy) {
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        String boardName = board.map(Board::getName).orElse("Unknown Board");

        if ("ADMIN".equals(triggeredBy.getRole())) {
            // ADMIN updates task - notify assigned USER only (if task is assigned to someone else)
            if (task.getAssignedTo() != null &&
                    shouldNotifyUser(task.getAssignedTo(), triggeredBy.getUsername()) &&
                    !"ADMIN".equals(task.getAssignedTo())) {

                String message = String.format("Task '%s' assigned to you has been updated in board '%s' to '%s'", task.getTitle(), boardName, task.getStatus());
                Notification userNotification = new Notification(
                        message, "TASK_UPDATED", task.getId(), task.getTitle(),
                        task.getBoardId(), boardName, task.getAssignedTo(), triggeredBy.getUsername()
                );
                safeSaveNotification(userNotification);
            }
        } else {
            // USER updates task - notify ADMIN only
            String message = String.format("Task '%s' updated by %s in board '%s' to '%s'", task.getTitle(), triggeredBy.getUsername(), boardName, task.getStatus());
            Notification adminNotification = new Notification(
                    message, "TASK_UPDATED", task.getId(), task.getTitle(),
                    task.getBoardId(), boardName, "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when task is assigned
    public void assignTaskNotification(Task task, String previousAssignee, User triggeredBy) {
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        String boardName = board.map(Board::getName).orElse("Unknown Board");

        if ("ADMIN".equals(triggeredBy.getRole())) {
            // ADMIN assigns task - notify the assigned USER only (if different from admin)
            if (task.getAssignedTo() != null &&
                    shouldNotifyUser(task.getAssignedTo(), triggeredBy.getUsername()) &&
                    !"ADMIN".equals(task.getAssignedTo())) {

                String message = String.format("Task '%s' has been assigned to you in board '%s' to '%s'", task.getTitle(), boardName, task.getStatus());
                Notification userNotification = new Notification(
                        message, "TASK_ASSIGNED", task.getId(), task.getTitle(),
                        task.getBoardId(), boardName, task.getAssignedTo(), triggeredBy.getUsername()
                );
                safeSaveNotification(userNotification);
            }
        } else {
            // USER assigns task - notify ADMIN only
            String message = String.format("Task '%s' assigned to %s by %s in board '%s' to '%s'",
                    task.getTitle(), task.getAssignedTo(), triggeredBy.getUsername(), boardName, task.getStatus());
            Notification adminNotification = new Notification(
                    message, "TASK_ASSIGNED", task.getId(), task.getTitle(),
                    task.getBoardId(), boardName, "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when task is deleted
    public void deleteTaskNotification(Task task, User triggeredBy) {
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        String boardName = board.map(Board::getName).orElse("Unknown Board");

        if ("ADMIN".equals(triggeredBy.getRole())) {
            // ADMIN archives task - notify assigned USER only (if task was assigned to someone else)
            if (task.getAssignedTo() != null &&
                    shouldNotifyUser(task.getAssignedTo(), triggeredBy.getUsername()) &&
                    !"ADMIN".equals(task.getAssignedTo())) {

                String message = String.format("Task '%s' assigned to you has been archived in board '%s'", task.getTitle(), boardName);
                Notification userNotification = new Notification(
                        message, "TASK_ARCHIVED", task.getId(), task.getTitle(),
                        task.getBoardId(), boardName, task.getAssignedTo(), triggeredBy.getUsername()
                );
                safeSaveNotification(userNotification);
            }
        } else {
            // USER archives task - notify ADMIN only
            String message = String.format("Task '%s' archived by %s in board '%s'", task.getTitle(), triggeredBy.getUsername(), boardName);
            Notification adminNotification = new Notification(
                    message, "TASK_ARCHIVED", task.getId(), task.getTitle(),
                    task.getBoardId(), boardName, "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when board is deleted
    public void deleteBoardNotification(Board board, User triggeredBy) {
        // Only notify if USER deletes board (notify ADMIN)
        // Don't notify if ADMIN deletes board
        if ("USER".equals(triggeredBy.getRole())) {
            String message = String.format("Board '%s' has been deleted by %s", board.getName(), triggeredBy.getUsername());
            Notification adminNotification = new Notification(
                    message, "BOARD_DELETED", null, null,
                    board.getId(), board.getName(), "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Create notification when board is updated
    public void updateBoardNotification(Board board, User triggeredBy) {
        // Only notify if USER updates board (notify ADMIN)
        // Don't notify if ADMIN updates board
        if ("USER".equals(triggeredBy.getRole())) {
            String message = String.format("Board '%s' has been updated by %s", board.getName(), triggeredBy.getUsername());
            Notification adminNotification = new Notification(
                    message, "BOARD_UPDATED", null, null,
                    board.getId(), board.getName(), "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    // Add this method to your NotificationService class
    public void restoreTaskNotification(Task task, User triggeredBy) {
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        String boardName = board.map(Board::getName).orElse("Unknown Board");

        if ("ADMIN".equals(triggeredBy.getRole())) {
            // ADMIN restores task - notify assigned USER only (if task is assigned to someone else)
            if (task.getAssignedTo() != null &&
                    shouldNotifyUser(task.getAssignedTo(), triggeredBy.getUsername()) &&
                    !"ADMIN".equals(task.getAssignedTo())) {

                String message = String.format("Task '%s' assigned to you has been restored in board '%s'", task.getTitle(), boardName);
                Notification userNotification = new Notification(
                        message, "TASK_RESTORED", task.getId(), task.getTitle(),
                        task.getBoardId(), boardName, task.getAssignedTo(), triggeredBy.getUsername()
                );
                safeSaveNotification(userNotification);
            }
        } else {
            // USER restores task - notify ADMIN only
            String message = String.format("Task '%s' restored by %s in board '%s'", task.getTitle(), triggeredBy.getUsername(), boardName);
            Notification adminNotification = new Notification(
                    message, "TASK_RESTORED", task.getId(), task.getTitle(),
                    task.getBoardId(), boardName, "ADMIN", triggeredBy.getUsername()
            );
            safeSaveNotification(adminNotification);
        }
    }

    private void safeSaveNotification(Notification newNotification) {
        List<Notification> existing = notificationRepository.findByTargetUserOrderByCreatedAtDesc(newNotification.getTargetUser());

        boolean duplicateExists = existing.stream().anyMatch(n ->
                n.getType().equals(newNotification.getType()) &&
                        ((n.getTaskId() == null && newNotification.getTaskId() == null) ||
                                (n.getTaskId() != null && n.getTaskId().equals(newNotification.getTaskId()))) &&
                        !n.isRead() &&
                        n.getMessage().equals(newNotification.getMessage()) // even better
        );

        if (!duplicateExists) {
            notificationRepository.save(newNotification);
        } else {
            System.out.println("[SKIPPED] Duplicate notification for user: " + newNotification.getTargetUser() + " — " + newNotification.getMessage());
        }
    }
}