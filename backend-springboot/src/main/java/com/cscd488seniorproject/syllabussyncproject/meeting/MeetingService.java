package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    public Meeting createMeeting(Meeting meeting) {
        meeting.setCreatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    public List<Meeting> getMeetingsByClassCode(String classCode) {
        return meetingRepository.findByClassCode(classCode);
    }

    public Optional<Meeting> getMeetingById(Long meetingId) {
        return meetingRepository.findById(meetingId);
    }

    public Meeting updateMeeting(Long meetingId, Meeting updatedMeeting) {
    Optional<Meeting> existing = meetingRepository.findById(meetingId);
    if (existing.isPresent()) {
        Meeting meeting = existing.get();
        meeting.setClassCode(updatedMeeting.getClassCode());
        meeting.setQuarter(updatedMeeting.getQuarter());
        meeting.setYear(updatedMeeting.getYear());
        meeting.setMeetingDate(updatedMeeting.getMeetingDate());
        meeting.setStartTime(updatedMeeting.getStartTime());
        meeting.setEndTime(updatedMeeting.getEndTime());
        meeting.setStatus(updatedMeeting.getStatus());
        meeting.setNotes(updatedMeeting.getNotes());
        return meetingRepository.save(meeting);
    }
    return null;
}

    public void deleteMeeting(Long meetingId) {
        meetingRepository.deleteById(meetingId);
    }

    public List<Meeting> getUpcomingMeetings(String classCode) {
        return meetingRepository.findByClassCodeAndMeetingDateBetween(
            classCode, 
            LocalDate.now(), 
            LocalDate.now().plusDays(30)
        );
    }

    public List<Meeting> getMeetingsByRequesterId(String requesterId) {
        return meetingRepository.findByRequesterId(requesterId);
    }

    public List<Meeting> getMeetingsByRecipientId(String recipientId) {
        return meetingRepository.findByRecipientId(recipientId);
    }

    public List<Meeting> getPendingMeetings(String recipientId) {
        List<Meeting> meetings = meetingRepository.findByRecipientId(recipientId);
        return meetings.stream().filter(m -> "PENDING".equals(m.getStatus())).toList();
    }
}