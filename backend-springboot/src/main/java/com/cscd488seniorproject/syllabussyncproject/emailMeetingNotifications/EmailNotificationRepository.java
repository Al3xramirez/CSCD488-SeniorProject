package com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotificationEntity, Long> {
    List<EmailNotificationEntity> findByStatus(EmailNotificationEntity.NotificationStatus status);
    List<EmailNotificationEntity> findByMeetingId(Long meetingId);
    List<EmailNotificationEntity> findByRecipientEmail(String recipientEmail);
}