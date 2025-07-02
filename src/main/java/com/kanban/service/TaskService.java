package com.kanban.service;

import com.kanban.model.Board;
import com.kanban.model.Task;
import com.kanban.model.User;
import com.kanban.repository.BoardRepository;
import com.kanban.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    BoardService boardService;

    @Autowired
    NotificationService notificationService;

    public List<Task> getTasksByBoard(String boardId, User user) {
        Optional<Board> board = boardRepository.findById(boardId);
        if (board.isPresent()) {
            // Admin can see all tasks in any board
            if ("ADMIN".equals(user.getRole())) {
                return taskRepository.findByBoardId(boardId);
            }

            // Regular users can see tasks in boards they own OR tasks assigned to them
            if (board.get().getCreatedBy().equals(user.getUsername())) {
                return taskRepository.findByBoardId(boardId);
            }

            // Or if they have tasks assigned to them in this board
            List<Task> allTasksInBoard = taskRepository.findByBoardId(boardId);
            return allTasksInBoard.stream()
                    .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(user.getUsername()))
                    .collect(Collectors.toList());
        }
        throw new RuntimeException("Board not found or access denied");
    }

    public List<Task> getAllTasks(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return taskRepository.findAll();
        }

        // For regular users, show only tasks assigned to them
        return taskRepository.findByAssignedTo(user.getUsername());
    }

    public Optional<Task> getTaskById(String id, User user) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent() && canAccessTask(task.get(), user)) {
            return task;
        }
        return Optional.empty();
    }

    public Task createTask(Task task, User user) {
        // Only admin can create tasks
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Access denied - only admin can create tasks");
        }

        Optional<Board> board = boardRepository.findById(task.getBoardId());
        if (!board.isPresent()) {
            throw new RuntimeException("Board not found");
        }

        task.setCreatedBy(user.getUsername());
        Task createdTask = taskRepository.save(task);

        // Create notification
        notificationService.createTaskNotification(createdTask, user);

        return createdTask;
    }

    @PreAuthorize("hasRole('ADMIN') or @taskService.canModifyTask(#id, authentication.principal)")
    public Task updateTask(String id, Task updatedTask, User user) {
        Optional<Task> existingTask = taskRepository.findById(id);
        if (existingTask.isPresent() && canModifyTask(id, user)) {
            Task task = existingTask.get();
            String previousAssignee = task.getAssignedTo();

            task.setTitle(updatedTask.getTitle());
            task.setDescription(updatedTask.getDescription());
            task.setStatus(updatedTask.getStatus());
            task.setPriority(updatedTask.getPriority());
            task.setAssignedTo(updatedTask.getAssignedTo());

            Task savedTask = taskRepository.save(task);

            // Create notifications
            notificationService.updateTaskNotification(savedTask, user);
            if (!Objects.equals(previousAssignee, savedTask.getAssignedTo())) {
                notificationService.assignTaskNotification(savedTask, previousAssignee, user);
            }

            return savedTask;
        }
        throw new RuntimeException("Task not found or access denied");
    }

    // New method for status-only updates (for regular users)
    public Task updateTaskStatusOnly(String id, String newStatus, User user) {
        Optional<Task> existingTask = taskRepository.findById(id);
        if (!existingTask.isPresent()) {
            throw new RuntimeException("Task not found");
        }

        Task task = existingTask.get();

        // Check if user can update this task status (only assigned user)
        if (!task.getAssignedTo().equals(user.getUsername())) {
            throw new RuntimeException("Access denied - you can only update tasks assigned to you");
        }

        task.setStatus(newStatus);
        Task savedTask = taskRepository.save(task);

        // Create notification for status update
        notificationService.updateTaskNotification(savedTask, user);

        return savedTask;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTask(String id, User user) {
        // Only admin can delete tasks
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Access denied - only admin can delete tasks");
        }

        Optional<Task> taskToDelete = taskRepository.findById(id);
        if (taskToDelete.isPresent()) {
            Task task = taskToDelete.get();

            // Instead of deleting, we'll archive the task
            task.setArchived(true);
            taskRepository.save(task);

            // Create notification before archiving
            notificationService.deleteTaskNotification(task, user);
        } else {
            throw new RuntimeException("Task not found");
        }
    }

    // Add new method to get archived tasks
    public List<Task> getArchivedTasks(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return taskRepository.findByIsArchived(true);
        }
        // Regular users can only see their own archived tasks
        return taskRepository.findByAssignedToAndIsArchived(user.getUsername(), true);
    }

    // Add new method to restore archived tasks
    // In TaskService.java
    public Task restoreTask(String id, User user) {
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setArchived(false);
            Task restoredTask = taskRepository.save(task);

            // Add notification for task restoration
            notificationService.restoreTaskNotification(restoredTask, user);

            return restoredTask;
        }
        throw new RuntimeException("Task not found");
    }

    // Enhanced access control for tasks
    public boolean canAccessTask(Task task, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        // User can access if they created the task (admin only creates)
        if (task.getCreatedBy().equals(user.getUsername())) {
            return true;
        }

        // User can access if task is assigned to them
        if (task.getAssignedTo() != null && task.getAssignedTo().equals(user.getUsername())) {
            return true;
        }

        // User can access if they own the board where this task exists (admin only owns boards)
        Optional<Board> board = boardRepository.findById(task.getBoardId());
        if (board.isPresent() && board.get().getCreatedBy().equals(user.getUsername())) {
            return true;
        }

        return false;
    }

    public boolean canModifyTask(String taskId, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return true; // Admin can modify any task
        }

        Optional<Task> task = taskRepository.findById(taskId);
        if (!task.isPresent()) {
            return false;
        }

        // Regular users can only modify (update status) tasks assigned to them
        return task.get().getAssignedTo() != null && task.get().getAssignedTo().equals(user.getUsername());
    }

    // Check if user can create tasks in a specific board
    public boolean canCreateTaskInBoard(String boardId, User user) {
        // Only admin can create tasks
        return "ADMIN".equals(user.getRole());
    }

    // Get all boards that are accessible to the user (including those with assigned tasks)
    public List<Board> getAccessibleBoards(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return boardRepository.findAll();
        }

        // Get boards where user has assigned tasks
        List<Task> assignedTasks = taskRepository.findByAssignedTo(user.getUsername());
        List<String> boardIdsWithAssignedTasks = assignedTasks.stream()
                .map(Task::getBoardId)
                .distinct()
                .collect(Collectors.toList());

        // Return boards where user has assigned tasks
        return boardRepository.findAllById(boardIdsWithAssignedTasks);
    }



    // Check if user can update task status (for drag and drop)
    public boolean canUpdateTaskStatus(String taskId, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        Optional<Task> task = taskRepository.findById(taskId);
        if (!task.isPresent()) {
            return false;
        }

        // Regular users can update status only for tasks assigned to them
        return task.get().getAssignedTo() != null && task.get().getAssignedTo().equals(user.getUsername());
    }
}