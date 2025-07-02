package com.kanban.repository;

import com.kanban.model.Board;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends MongoRepository<Board, String> {

    // Find boards created by a specific user
    List<Board> findByCreatedBy(String createdBy);

    // Find boards by multiple IDs (for boards where user has assigned tasks)
    List<Board> findAllById(Iterable<String> ids);

    // Custom query to find boards where user is either creator or has tasks assigned
//    @Query("{ $or: [ { 'createdBy': ?0 }, { '_id': { $in: ?1 } } ] }")
//    List<Board> findBoardsAccessibleToUser(String username, List<String> boardIdsWithTasks);
}