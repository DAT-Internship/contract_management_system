package com.cms.demo.repository;

import com.cms.demo.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // Admin: all emails - newest first
    List<EmailLog> findAllByOrderByCreatedAtDesc();

    // Others: only emails sent to their own address (case-insensitive)
    List<EmailLog> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);

    // Sidebar badge: emails received but not read yet (legacy 'SENT' = unread)
    @Query("""
        SELECT COUNT(e) FROM EmailLog e
        WHERE LOWER(e.recipientEmail) = LOWER(:email)
          AND e.status IN ('SENT_UNREAD', 'SENT')
        """)
    long countUnreadForRecipient(@Param("email") String email);

    // Recipient opens the email -> mark as read (only the recipient can do this)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EmailLog e
        SET e.status = 'SENT_READ'
        WHERE e.emailLogId = :id
          AND LOWER(e.recipientEmail) = LOWER(:email)
          AND e.status IN ('SENT_UNREAD', 'SENT')
        """)
    int markAsRead(@Param("id") Long id, @Param("email") String email);
    
    
}