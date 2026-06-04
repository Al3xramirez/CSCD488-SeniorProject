package com.cscd488seniorproject.syllabussyncproject.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    List<NotificationEntity> findByRecipientEmailAndReadFalse(String recipientEmail);

    long deleteByRecipientEmail(String recipientEmail);
}
