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

    // Sidebar badge: emails addressed to me that I have not opened yet
    // (includes FAILED / PENDING ones, so a mail-server problem is still visible)
    @Query("""
        SELECT COUNT(e) FROM EmailLog e
        WHERE LOWER(e.recipientEmail) = LOWER(:email)
          AND e.readAt IS NULL
          AND e.status <> 'CANCELLED'
        """)
    long countUnreadForRecipient(@Param("email") String email);

    // Recipient opens the email -> mark as read (only the recipient can do this).
    // Delivered mails move to SENT_READ; FAILED / PENDING keep their status and only get read_at.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EmailLog e
        SET e.readAt = CURRENT_TIMESTAMP,
            e.status = CASE WHEN e.status IN ('SENT_UNREAD', 'SENT') THEN 'SENT_READ' ELSE e.status END
        WHERE e.emailLogId = :id
          AND LOWER(e.recipientEmail) = LOWER(:email)
          AND e.readAt IS NULL
        """)
    int markAsRead(@Param("id") Long id, @Param("email") String email);
}
