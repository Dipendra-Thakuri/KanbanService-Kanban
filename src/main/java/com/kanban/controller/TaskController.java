package com.kanban.controller;

import com.kanban.model.Task;
import com.kanban.model.User;
import com.kanban.service.TaskService;
import com.kanban.service.BoardService;
import com.kanban.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "http://localhost:3000")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Task> tasks = taskService.getAllTasks(user);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Task>> getTasksByBoard(@PathVariable String boardId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<Task> tasks = taskService.getTasksByBoard(boardId, user);
            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Task> task = taskService.getTaskById(id, user);
        return task.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskRequest request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Only ADMIN can create tasks
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            Task task = new Task(
                    request.getTitle(),
                    request.getDescription(),
                    request.getStatus() != null ? request.getStatus() : "To Do",
                    request.getPriority() != null ? request.getPriority() : "Medium",
                    request.getAssignedTo(),
                    user.getUsername(),
                    request.getBoardId()
            );
            Task createdTask = taskService.createTask(task, user);

            // Add notification for task creation
            notificationService.createTaskNotification(createdTask, user);

            return ResponseEntity.ok(createdTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id, @RequestBody TaskRequest request,
                                           Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Get the original task to check permissions and changes
            Optional<Task> originalTaskOpt = taskService.getTaskById(id, user);
            if (!originalTaskOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Task originalTask = originalTaskOpt.get();
            String previousAssignee = originalTask.getAssignedTo();
            boolean assignmentChanged = !java.util.Objects.equals(previousAssignee, request.getAssignedTo());

            if (isAdmin(authentication)) {
                // Admin can update all fields
                Task updatedTask = new Task(
                        request.getTitle(),
                        request.getDescription(),
                        request.getStatus(),
                        request.getPriority(),
                        request.getAssignedTo(),
                        originalTask.getCreatedBy(), // Preserve original creator
                        request.getBoardId()
                );
                Task task = taskService.updateTask(id, updatedTask, user);

                // Add notifications for task update
                if (assignmentChanged && request.getAssignedTo() != null) {
                    notificationService.assignTaskNotification(task, previousAssignee, user);
                } else {
                    notificationService.updateTaskNotification(task, user);
                }

                return ResponseEntity.ok(task);
            } else {
                // Regular user can only update status of tasks assigned to them
                if (!user.getUsername().equals(originalTask.getAssignedTo())) {
                    return ResponseEntity.status(403).build(); // Forbidden
                }

                // Only allow status updates for regular users
                if (!originalTask.getTitle().equals(request.getTitle()) ||
                        !originalTask.getDescription().equals(request.getDescription()) ||
                        !originalTask.getPriority().equals(request.getPriority()) ||
                        assignmentChanged ||
                        !originalTask.getBoardId().equals(request.getBoardId())) {
                    return ResponseEntity.status(403).build(); // Forbidden - only status can be changed
                }

                // Create updated task with only status changed
                Task updatedTask = new Task(
                        originalTask.getTitle(),
                        originalTask.getDescription(),
                        request.getStatus(),
                        originalTask.getPriority(),
                        originalTask.getAssignedTo(),
                        originalTask.getCreatedBy(),
                        originalTask.getBoardId()
                );
                Task task = taskService.updateTaskStatusOnly(id, request.getStatus(), user);

                // Add notification for status update
                notificationService.updateTaskNotification(task, user);

                return ResponseEntity.ok(task);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Only ADMIN can delete tasks
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            // Get task details before deletion for notification
            Optional<Task> taskOptional = taskService.getTaskById(id, user);

            if (taskOptional.isPresent()) {
                Task taskToDelete = taskOptional.get();
                taskService.deleteTask(id, user);

                // Add notification for task deletion
                notificationService.deleteTaskNotification(taskToDelete, user);
            } else {
                taskService.deleteTask(id, user);
            }

            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if user has admin role
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ADMIN") || authority.equals("ROLE_ADMIN"));
    }

    // Add this method to your TaskController class

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable String id, @RequestBody StatusUpdateRequest request,
                                                 Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Check if user can update this task status
            if (!taskService.canUpdateTaskStatus(id, user)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            Task updatedTask = taskService.updateTaskStatusOnly(id, request.getStatus(), user);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/archived")
    public ResponseEntity<List<Task>> getArchivedTasks(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Task> tasks = taskService.getArchivedTasks(user);
        return ResponseEntity.ok(tasks);
    }

    // New endpoint to restore archived task
    @PutMapping("/{id}/restore")
    public ResponseEntity<Task> restoreTask(@PathVariable String id, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Task restoredTask = taskService.restoreTask(id, user);
            return ResponseEntity.ok(restoredTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Add this DTO class inside TaskController
    public static class StatusUpdateRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // DTO class for request
    public static class TaskRequest {
        private String title;
        private String description;
        private String status;
        private String priority;
        private String assignedTo;
        private String boardId;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

        public String getBoardId() { return boardId; }
        public void setBoardId(String boardId) { this.boardId = boardId; }
    }
}