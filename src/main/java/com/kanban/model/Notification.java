package com.kanban.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String message;
    private String type; // TASK_CREATED, TASK_UPDATED, TASK_ASSIGNED, TASK_DELETED, BOARD_CREATED, BOARD_UPDATED, BOARD_DELETED
    private String taskId;
    private String taskTitle;
    private String boardId;
    private String boardName;
    private String targetUser; // who should see this notification
    private String triggeredBy; // who created this notification
    private boolean isRead;

    @CreatedDate
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String message, String type, String taskId, String taskTitle,
                        String boardId, String boardName, String targetUser, String triggeredBy) {
        this.message = message;
        this.type = type;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.boardId = boardId;
        this.boardName = boardName;
        this.targetUser = targetUser;
        this.triggeredBy = triggeredBy;
        this.isRead = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }

    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", taskId='" + taskId + '\'' +
                ", taskTitle='" + taskTitle + '\'' +
                ", boardId='" + boardId + '\'' +
                ", boardName='" + boardName + '\'' +
                ", targetUser='" + targetUser + '\'' +
                ", triggeredBy='" + triggeredBy + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}