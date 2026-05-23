package com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.meeting.MeetingEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;
    private final UserAccountRepository userAccountRepository;

    public EmailController(EmailService emailService, UserAccountRepository userAccountRepository) {
        this.emailService = emailService;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/class-members/{classId}")
    public ResponseEntity<List<UserAccountEntity>> getClassMembers(@PathVariable Long classId) {
        List<UserAccountEntity> members = userAccountRepository.findClassMembers(classId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/send-meeting-notification")
    public ResponseEntity<String> sendMeetingNotification(
            @RequestBody MeetingEntity meeting,
            @RequestParam String recipientUserId) {
        
        if (meeting == null || meeting.getMeetingDate() == null || meeting.getClassCode() == null || recipientUserId == null) {
            return ResponseEntity.badRequest().body("Invalid meeting or recipient");
        }

        emailService.sendMeetingNotification(meeting, recipientUserId);
        return ResponseEntity.ok("Email sent successfully to user: " + recipientUserId);
    }
}