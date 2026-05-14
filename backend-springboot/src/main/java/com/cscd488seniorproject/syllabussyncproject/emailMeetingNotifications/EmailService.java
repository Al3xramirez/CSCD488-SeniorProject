package com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.meeting.Meeting;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserAccountRepository userAccountRepository;
    private static final String SENDER_EMAIL = "noreply@syllabussync.com";

    public EmailService(JavaMailSender mailSender, UserAccountRepository userAccountRepository) {
        this.mailSender = mailSender;
        this.userAccountRepository = userAccountRepository;
    }

    public void sendMeetingNotification(Meeting meeting, String recipientUserId) {
        if (meeting == null || meeting.getMeetingDate() == null || meeting.getClassCode() == null) {
            throw new IllegalArgumentException("Invalid meeting");
        }

        String recipientEmail = getRecipientEmailByUserId(recipientUserId);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER_EMAIL);
        message.setTo(recipientEmail);
        message.setSubject("Meeting Scheduled: " + meeting.getMeetingDate());
        message.setText(buildMeetingEmailBody(meeting));

        mailSender.send(message);
    }

    private String getRecipientEmailByUserId(String userId) {
        UserAccountEntity user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getEmail();
    }

    private String buildMeetingEmailBody(Meeting meeting) {
        return String.format(
            "A new meeting has been scheduled:\n\n" +
            "Date: %s\n" +
            "Start Time: %s\n" +
            "End Time: %s\n" +
            "Class ID: %d\n\n" +
            "Please mark your calendar accordingly.",
            meeting.getMeetingDate(),
            meeting.getStartTime(),
            meeting.getEndTime(),
            meeting.getClassCode()
        );
    }
}