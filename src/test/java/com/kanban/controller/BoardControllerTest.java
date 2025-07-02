package com.kanban.controller;

import com.kanban.model.Board;
import com.kanban.model.User;
import com.kanban.service.BoardService;
import com.kanban.service.NotificationService;
import com.kanban.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardService boardService;

    @Mock
    private TaskService taskService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BoardController boardController;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc with the standalone controller
        mockMvc = MockMvcBuilders.standaloneSetup(boardController).build();

        testUser = new User("testUser", "test@example.com", "USER");
    }

    @Test
    void getAllBoards_ShouldReturnBoards() throws Exception {
        Board board = new Board("Test Board", "Description", "testUser", Arrays.asList("To Do", "Done"));
        when(boardService.getAllBoards(any(User.class))).thenReturn(Collections.singletonList(board));
        when(authentication.getPrincipal()).thenReturn(testUser);

        mockMvc.perform(get("/boards")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Board"));
    }

    @Test
    void getBoardById_WhenExists_ShouldReturnBoard() throws Exception {
        Board board = new Board("Test Board", "Description", "testUser", Arrays.asList("To Do", "Done"));
        when(boardService.getBoardById(anyString(), any(User.class))).thenReturn(Optional.of(board));
        when(authentication.getPrincipal()).thenReturn(testUser);

        mockMvc.perform(get("/boards/123")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Board"));
    }

    @Test
    void getBoardById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(boardService.getBoardById(anyString(), any(User.class))).thenReturn(Optional.empty());
        when(authentication.getPrincipal()).thenReturn(testUser);

        mockMvc.perform(get("/boards/123")
                        .principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBoard_WithValidRequest_ShouldReturnCreatedBoard() throws Exception {
        Board createdBoard = new Board("New Board", "Description", "testUser", Arrays.asList("To Do", "Done"));
        when(boardService.createBoard(any(Board.class), any(User.class))).thenReturn(createdBoard);
        when(authentication.getPrincipal()).thenReturn(testUser);

        String requestBody = "{\"name\":\"New Board\",\"description\":\"Description\",\"columns\":[\"To Do\",\"Done\"]}";

        mockMvc.perform(post("/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Board"));
    }

    @Test
    void updateBoard_WhenExists_ShouldReturnUpdatedBoard() throws Exception {
        Board updatedBoard = new Board("Updated Board", "New Description", "testUser",
                Arrays.asList("To Do", "In Progress", "Done"));
        when(boardService.updateBoard(anyString(), any(Board.class), any(User.class))).thenReturn(updatedBoard);
        when(authentication.getPrincipal()).thenReturn(testUser);

        String requestBody = "{\"name\":\"Updated Board\",\"description\":\"New Description\"," +
                "\"columns\":[\"To Do\",\"In Progress\",\"Done\"]}";

        mockMvc.perform(put("/boards/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Board"));
    }

    @Test
    void updateBoard_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(boardService.updateBoard(anyString(), any(Board.class), any(User.class))).thenThrow(new RuntimeException());
        when(authentication.getPrincipal()).thenReturn(testUser);

        String requestBody = "{\"name\":\"Updated Board\",\"description\":\"New Description\"}";

        mockMvc.perform(put("/boards/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBoard_WhenExists_ShouldReturnOk() throws Exception {
        when(authentication.getPrincipal()).thenReturn(testUser);

        mockMvc.perform(delete("/boards/123")
                        .principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void getAccessibleBoards_ShouldReturnBoards() throws Exception {
        Board board = new Board("Accessible Board", "Description", "testUser", Arrays.asList("To Do", "Done"));
        when(taskService.getAccessibleBoards(any(User.class))).thenReturn(Collections.singletonList(board));
        when(authentication.getPrincipal()).thenReturn(testUser);

        mockMvc.perform(get("/boards/accessible")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Accessible Board"));
    }

    @Test
    void createBoard_WithDefaultColumns_ShouldUseDefaultColumns() throws Exception {
        Board createdBoard = new Board("New Board", "Description", "testUser",
                Arrays.asList("To Do", "In Progress", "Done"));
        when(boardService.createBoard(any(Board.class), any(User.class))).thenReturn(createdBoard);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // Request without columns
        String requestBody = "{\"name\":\"New Board\",\"description\":\"Description\"}";

        mockMvc.perform(post("/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(3));
    }
}