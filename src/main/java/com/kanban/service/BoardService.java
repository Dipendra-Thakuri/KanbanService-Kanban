package com.kanban.service;

import com.kanban.model.Board;
import com.kanban.model.User;
import com.kanban.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoardService {

    @Autowired
    BoardRepository boardRepository;

    // Return all boards if admin, otherwise only boards created by the user OR assigned to them
    public List<Board> getAllBoards(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return boardRepository.findAll();
        }
        // For regular users, show boards they created OR boards where they have tasks assigned
        return boardRepository.findByCreatedBy(user.getUsername());
    }

    // Return board if accessible by user (admin, creator, or has tasks in the board)
    public Optional<Board> getBoardById(String id, User user) {
        Optional<Board> board = boardRepository.findById(id);
        if (board.isPresent() && canAccessBoard(board.get(), user)) {
            return board;
        }
        return Optional.empty();
    }

    // Allow user to create board
    public Board createBoard(Board board, User user) {
        board.setCreatedBy(user.getUsername());
        return boardRepository.save(board);
    }

    // Allow admin or board owner to update board
    @PreAuthorize("hasRole('ADMIN') or @boardService.canModifyBoard(#id, authentication.principal)")
    public Board updateBoard(String id, Board updatedBoard, User user) {
        Optional<Board> existingBoard = boardRepository.findById(id);
        if (existingBoard.isPresent() && canModifyBoard(id, user)) {
            Board board = existingBoard.get();
            board.setName(updatedBoard.getName());
            board.setDescription(updatedBoard.getDescription());
            board.setColumns(updatedBoard.getColumns());
            return boardRepository.save(board);
        }
        throw new RuntimeException("Board not found or access denied");
    }

    // Allow admin or board owner to delete board
    @PreAuthorize("hasRole('ADMIN') or @boardService.canModifyBoard(#id, authentication.principal)")
    public void deleteBoard(String id, User user) {
        if (canModifyBoard(id, user)) {
            boardRepository.deleteById(id);
        } else {
            throw new RuntimeException("Access denied");
        }
    }

    // Enhanced access control - allow access if user is admin, creator, or has tasks in the board
    public boolean canAccessBoard(Board board, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        // User can access if they created the board
        if (board.getCreatedBy().equals(user.getUsername())) {
            return true;
        }

        // Note: If you want to allow users to see boards where they have assigned tasks,
        // you would need to inject TaskRepository and check for tasks assigned to the user
        // For now, keeping it simple - users can only see boards they created

        return false;
    }

    // Helper for @PreAuthorize - access by boardId
    public boolean canAccessBoard(String boardId, User user) {
        Optional<Board> board = boardRepository.findById(boardId);
        return board.isPresent() && canAccessBoard(board.get(), user);
    }

    // Used for @PreAuthorize to check update/delete permission
    public boolean canModifyBoard(String boardId, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }
        Optional<Board> board = boardRepository.findById(boardId);
        return board.isPresent() && board.get().getCreatedBy().equals(user.getUsername());
    }

    // New method to check if a board exists and is accessible for task creation
    public boolean canCreateTaskInBoard(String boardId, User user) {
        Optional<Board> board = boardRepository.findById(boardId);
        if (!board.isPresent()) {
            return false;
        }

        // Admin can create tasks in any board
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        // Users can create tasks in boards they own
        // OR in boards where they already have tasks (if you want this behavior)
        return board.get().getCreatedBy().equals(user.getUsername());
    }
}