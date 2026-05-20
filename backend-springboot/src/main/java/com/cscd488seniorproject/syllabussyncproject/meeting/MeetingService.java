package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    public MeetingEntity createMeeting(MeetingEntity meeting) {
        meeting.setCreatedAt(LocalDateTime.now());
        return meetingRepository.save(meeting);
    }

    public List<MeetingEntity> getMeetingsByClassCode(String classCode) {
        return meetingRepository.findByClassCode(classCode);
    }

    public Optional<MeetingEntity> getMeetingById(Long meetingId) {
        return meetingRepository.findById(meetingId);
    }

    public MeetingEntity updateMeeting(Long meetingId, MeetingEntity updatedMeeting) {
        Optional<MeetingEntity> existing = meetingRepository.findById(meetingId);
        if (existing.isPresent()) {
            MeetingEntity meeting = existing.get();
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

    public List<MeetingEntity> getUpcomingMeetings(String classCode) {
        return meetingRepository.findByClassCodeAndMeetingDateBetween(
            classCode, 
            LocalDate.now(), 
            LocalDate.now().plusDays(30)
        );
    }

    public List<MeetingEntity> getMeetingsByRequesterId(String requesterId) {
        return meetingRepository.findByRequesterId(requesterId);
    }

    public List<MeetingEntity> getMeetingsByRecipientId(String recipientId) {
        return meetingRepository.findByRecipientId(recipientId);
    }

    public List<MeetingEntity> getPendingMeetings(String recipientId) {
        List<MeetingEntity> meetings = meetingRepository.findByRecipientId(recipientId);
        return meetings.stream()
            .filter(m -> "PENDING".equals(m.getStatus()))
            .toList();
    }

    public List<MeetingEntity> createRecurringMeetings(RecurringMeetingRequest request) {
        List<MeetingEntity> createdMeetings = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        String currentUserEmail = authentication.getName();
        
        while (!currentDate.isAfter(request.getEndDate())) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String dayName = dayOfWeek.toString();
            
            if (request.getDaysOfWeek().contains(dayName)) {
                
                MeetingEntity meeting = new MeetingEntity();
                meeting.setClassCode(request.getClassCode());
                meeting.setMeetingDate(currentDate);
                meeting.setStartTime(request.getStartTime());
                meeting.setEndTime(request.getEndTime());
                meeting.setRequesterId(currentUserEmail);
                meeting.setRecipientId(request.getRecipientId());
                meeting.setNotes(request.getNotes());
                meeting.setStatus("CONFIRMED"); //only used for class lectures/officehours so we can set status to confirmed
                
                MeetingEntity savedMeeting = createMeeting(meeting);
                createdMeetings.add(savedMeeting);
            }

            currentDate = currentDate.plusDays(1);
        }

        return createdMeetings;
    }

    public List<MeetingEntity> getAllMeetings() {
        return meetingRepository.findAll();
    }

    public List<MeetingEntity> getMeetingsForUserByDate(String userEmail, String date) {
        LocalDate localDate = LocalDate.parse(date);
        return meetingRepository.findByUserIdAndDateBetween(userEmail, localDate, localDate);
    }
}