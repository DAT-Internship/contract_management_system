package com.cms.demo.repository;

import com.cms.demo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    long countByUser_UserIdAndIsReadFalse(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Notification n
           SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
         WHERE n.notificationId = :id
           AND n.user.userId = :userId
           AND n.isRead = false
        """)
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Notification n
           SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
         WHERE n.user.userId = :userId
           AND n.isRead = false
        """)
    int markAllRead(@Param("userId") Long userId);
}