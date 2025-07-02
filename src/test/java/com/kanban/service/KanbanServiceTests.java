package com.kanban.service;

import com.kanban.model.Board;
import com.kanban.model.Notification;
import com.kanban.model.Task;
import com.kanban.model.User;
import com.kanban.repository.BoardRepository;
import com.kanban.repository.NotificationRepository;
import com.kanban.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KanbanServiceTests {

    // Shared test data
    private User adminUser;
    private User regularUser;
    private Board board;
    private Task task;
    private Notification notification;

    // Mock repositories
    @Mock private BoardRepository boardRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private TaskRepository taskRepository;

    // Services under test
    private BoardService boardService;
    private NotificationService notificationService;
    private TaskService taskService;

    @BeforeEach
    public void setUp() {
        // Initialize test data
        adminUser = new User("admin", "admin@test.com", "ADMIN");
        regularUser = new User("user", "user@test.com", "USER");

        board = new Board("Test Board", "Description", regularUser.getUsername(),
                Arrays.asList("To Do", "In Progress", "Done"));
        board.setId("board1");

        task = new Task("Task 1", "Description", "To Do", "Medium",
                regularUser.getUsername(), adminUser.getUsername(), board.getId());
        task.setId("task1");

        notification = new Notification("Test", "TASK_CREATED",
                task.getId(), task.getTitle(), board.getId(), board.getName(),
                adminUser.getUsername(), regularUser.getUsername());
        notification.setId("notif1");

        // Initialize services with mocked dependencies
        boardService = new BoardService();
        boardService.boardRepository = boardRepository;

        notificationService = new NotificationService();
        notificationService.notificationRepository = notificationRepository;
        notificationService.boardRepository = boardRepository;

        taskService = new TaskService();
        taskService.taskRepository = taskRepository;
        taskService.boardRepository = boardRepository;
        taskService.boardService = boardService;
        taskService.notificationService = notificationService;
    }

    // BoardService Tests
    @Test
    public void testGetAllBoardsForAdmin() {
        when(boardRepository.findAll()).thenReturn(List.of(board));

        List<Board> result = boardService.getAllBoards(adminUser);

        assertEquals(1, result.size());
        verify(boardRepository).findAll();
    }

    @Test
    public void testCreateBoard() {
        when(boardRepository.save(any(Board.class))).thenReturn(board);

        Board result = boardService.createBoard(board, regularUser);

        assertEquals(board.getId(), result.getId());
        verify(boardRepository).save(board);
    }

    // NotificationService Tests
    @Test
    public void testGetNotificationsForAdmin() {
        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        List<Notification> result = notificationService.getNotifications(adminUser);

        assertEquals(1, result.size());
        verify(notificationRepository).findAll();
    }

    @Test
    public void testMarkAsRead() {
        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(notification));

        notificationService.markAsRead("notif1", adminUser);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    // TaskService Tests
    @Test
    public void testGetTasksByBoardForAdmin() {
        when(boardRepository.findById(board.getId())).thenReturn(Optional.of(board));
        when(taskRepository.findByBoardId(board.getId())).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByBoard(board.getId(), adminUser);

        assertEquals(1, result.size());
        verify(taskRepository).findByBoardId(board.getId());
    }

    @Test
    public void testCreateTaskByAdmin() {
        when(boardRepository.findById(board.getId())).thenReturn(Optional.of(board));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(task, adminUser);

        assertEquals(task.getId(), result.getId());
        verify(taskRepository).save(task);
    }

    @Test
    public void testUpdateTaskStatusOnly() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.updateTaskStatusOnly(task.getId(), "In Progress", regularUser);

        assertEquals("In Progress", result.getStatus());
        verify(taskRepository).save(task);
    }
}