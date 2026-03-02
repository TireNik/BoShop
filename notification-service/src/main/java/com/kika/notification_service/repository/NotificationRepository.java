package com.kika.notification_service.repository;

import com.kika.notification_service.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdAndSentFalse(Long userId);
}