package com.kanban.repository;

import com.kanban.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByTargetUserOrderByCreatedAtDesc(String targetUser);
    List<Notification> findByTargetUserAndIsReadOrderByCreatedAtDesc(String targetUser, boolean isRead);
    long countByTargetUserAndIsRead(String targetUser, boolean isRead);
}
