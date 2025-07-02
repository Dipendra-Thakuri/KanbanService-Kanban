package com.kanban.repository;

import com.kanban.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByBoardId(String boardId);
    List<Task> findByAssignedTo(String assignedTo);
//    List<Task> findByCreatedBy(String createdBy);
//    List<Task> findByBoardIdAndStatus(String boardId, String status);
    List<Task> findByIsArchived(boolean isArchived);
//    List<Task> findByBoardIdAndIsArchived(String boardId, boolean isArchived);

    List<Task> findByAssignedToAndIsArchived(String username, boolean b);
}