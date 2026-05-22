package com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.meeting.MeetingEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserAccountRepository userAccountRepository;
    private static final String SENDER_EMAIL = "syllabussyncproject@gmail.com";

    public EmailService(JavaMailSender mailSender, UserAccountRepository userAccountRepository) {
        this.mailSender = mailSender;
        this.userAccountRepository = userAccountRepository;
    }

    public void sendMeetingNotification(MeetingEntity meeting, String recipientUserId) {
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
        Optional<UserAccountEntity> byId = userAccountRepository.findById(userId);
        if (byId.isPresent()) return byId.get().getEmail();
        // Frontend passes email addresses directly, not user IDs
        return userAccountRepository.findByEmail(userId)
            .map(UserAccountEntity::getEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private String buildMeetingEmailBody(MeetingEntity meeting) {
        return String.format(
            "A new meeting has been scheduled with the following details:\n\n" +
            "Requested By: %s\n" +
            "Date: %s\n" +
            "Start Time: %s\n" +
            "End Time: %s\n" +
            "Class: %s\n\n" +
            "Please mark your calendar accordingly.",
            meeting.getRequesterId(),
            meeting.getMeetingDate(),
            meeting.getStartTime(),
            meeting.getEndTime(),
            meeting.getClassCode()
        );
    }
}