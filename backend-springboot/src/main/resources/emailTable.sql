CREATE TABLE email_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    meeting_id BIGINT NULL,
    FOREIGN KEY (meeting_id) REFERENCES meeting(MeetingID) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_created_at (created_at)
);

