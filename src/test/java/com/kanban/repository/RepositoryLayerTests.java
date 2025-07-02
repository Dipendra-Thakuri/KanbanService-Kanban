package com.kanban.repository;

import com.kanban.model.Board;
import com.kanban.model.Notification;
import com.kanban.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
public class RepositoryLayerTests {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    public void cleanup() {
        boardRepository.deleteAll();
        taskRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    // BoardRepository Tests
    @Test
    public void testBoardRepository() {
        // Create test data
        Board board = new Board("Test Board", "Description", "user1",
                Arrays.asList("To Do", "In Progress", "Done"));

        // Save and verify
        Board savedBoard = boardRepository.save(board);
        assertNotNull(savedBoard.getId());

        // Find by createdBy
        List<Board> boards = boardRepository.findByCreatedBy("user1");
        assertEquals(1, boards.size());
        assertEquals("Test Board", boards.get(0).getName());
    }

    // TaskRepository Tests
    @Test
    public void testTaskRepository() {
        // Create test data
        Task task = new Task("Task 1", "Description", "To Do", "Medium",
                "user1", "admin", "board1");

        // Save and verify
        Task savedTask = taskRepository.save(task);
        assertNotNull(savedTask.getId());

        // Find by boardId
        List<Task> tasks = taskRepository.findByBoardId("board1");
        assertEquals(1, tasks.size());
        assertEquals("Task 1", tasks.get(0).getTitle());

        // Find by assignedTo
        tasks = taskRepository.findByAssignedTo("user1");
        assertEquals(1, tasks.size());
    }

    // NotificationRepository Tests
    @Test
    public void testNotificationRepository() {
        // Create test data
        Notification notification = new Notification(
                "Test message", "TASK_CREATED", "task1", "Task 1",
                "board1", "Test Board", "user1", "admin"
        );

        // Save and verify
        Notification savedNotification = notificationRepository.save(notification);
        assertNotNull(savedNotification.getId());

        // Find by targetUser
        List<Notification> notifications = notificationRepository.findByTargetUserOrderByCreatedAtDesc("user1");
        assertEquals(1, notifications.size());
        assertEquals("Test message", notifications.get(0).getMessage());

        // Count unread
        long count = notificationRepository.countByTargetUserAndIsRead("user1", false);
        assertEquals(1, count);
    }
}