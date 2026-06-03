package com.cscd488seniorproject.syllabussyncproject.notification;

import com.cscd488seniorproject.syllabussyncproject.meeting.MeetingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void saveMeetingRequestNotification(MeetingEntity meeting) {
        if (meeting.getRecipientId() == null || meeting.getRecipientId().isBlank()) return;
        NotificationEntity n = new NotificationEntity();
        n.setRecipientEmail(meeting.getRecipientId());
        n.setType("MEETING_REQUEST");
        n.setMessage(String.format("New meeting request from %s on %s at %s",
            meeting.getRequesterId(), meeting.getMeetingDate(), fmtTime(meeting.getStartTime() != null ? meeting.getStartTime().toString() : "")));
        n.setMeetingId(meeting.getMeetingId());
        notificationRepository.save(n);
    }

    public void saveMeetingConfirmedNotification(MeetingEntity meeting) {
        if (meeting.getRequesterId() == null || meeting.getRequesterId().isBlank()) return;
        NotificationEntity n = new NotificationEntity();
        n.setRecipientEmail(meeting.getRequesterId());
        n.setType("MEETING_CONFIRMED");
        n.setMessage(String.format("Your meeting on %s at %s has been confirmed",
            meeting.getMeetingDate(), fmtTime(meeting.getStartTime() != null ? meeting.getStartTime().toString() : "")));
        n.setMeetingId(meeting.getMeetingId());
        notificationRepository.save(n);
    }

    public void saveMeetingDeclinedNotification(MeetingEntity meeting) {
        if (meeting.getRequesterId() == null || meeting.getRequesterId().isBlank()) return;
        NotificationEntity n = new NotificationEntity();
        n.setRecipientEmail(meeting.getRequesterId());
        n.setType("MEETING_DECLINED");
        n.setMessage(String.format("Your meeting on %s at %s was declined",
            meeting.getMeetingDate(), fmtTime(meeting.getStartTime() != null ? meeting.getStartTime().toString() : "")));
        n.setMeetingId(meeting.getMeetingId());
        notificationRepository.save(n);
    }

    private String fmtTime(String t) {
        if (t == null || t.isBlank()) return "";
        try {
            String[] parts = t.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String ampm = h < 12 ? "AM" : "PM";
            return String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, ampm);
        } catch (Exception e) {
            return t;
        }
    }
}
