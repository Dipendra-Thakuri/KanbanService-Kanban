package com.kanban.controller;

import com.kanban.model.Board;
import com.kanban.model.User;
import com.kanban.service.BoardService;
import com.kanban.service.TaskService;
import com.kanban.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/boards")
@CrossOrigin(origins = "http://localhost:3000")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Board>> getAllBoards(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // Enhanced logic to show boards where user has access
        List<Board> boards;
        if ("ADMIN".equals(user.getRole())) {
            boards = boardService.getAllBoards(user);
        } else {
            // For regular users, get boards they can access (owned + with assigned tasks)
            boards = taskService.getAccessibleBoards(user);
        }

        return ResponseEntity.ok(boards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable String id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Board> board = boardService.getBoardById(id, user);
        return board.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Board> createBoard(@RequestBody BoardRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // Default columns if not provided
        List<String> columns = request.getColumns() != null ? request.getColumns() :
                Arrays.asList("To Do", "In Progress", "Done");

        Board board = new Board(request.getName(), request.getDescription(), user.getUsername(), columns);
        Board createdBoard = boardService.createBoard(board, user);

        // Add notification for board creation
        notificationService.createBoardNotification(createdBoard, user);

        return ResponseEntity.ok(createdBoard);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(@PathVariable String id, @RequestBody BoardRequest request,
                                             Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Board updatedBoard = new Board(request.getName(), request.getDescription(), null, request.getColumns());
            Board board = boardService.updateBoard(id, updatedBoard, user);

            // Add notification for board update
            notificationService.updateBoardNotification(board, user);

            return ResponseEntity.ok(board);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String id, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            // Get board details before deletion for notification
            Optional<Board> boardOptional = boardService.getBoardById(id, user);
            if (boardOptional.isPresent()) {
                Board boardToDelete = boardOptional.get();
                boardService.deleteBoard(id, user);

                // Add notification for board deletion
                notificationService.deleteBoardNotification(boardToDelete, user);
            } else {
                boardService.deleteBoard(id, user);
            }

            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // New endpoint to get boards where user can create tasks
    @GetMapping("/accessible")
    public ResponseEntity<List<Board>> getAccessibleBoards(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Board> boards = taskService.getAccessibleBoards(user);
        return ResponseEntity.ok(boards);
    }

    // DTO class for request
    public static class BoardRequest {
        private String name;
        private String description;
        private List<String> columns;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }
}