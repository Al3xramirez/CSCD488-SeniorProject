package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "http://localhost:5173")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @PostMapping
    public ResponseEntity<Meeting> createMeeting(@RequestBody MeetingRequest request) {
        Meeting meeting = new Meeting();
        meeting.setClassCode(request.getClassCode());
        meeting.setQuarter(request.getQuarter());
        meeting.setYear(request.getYear());
        meeting.setRequesterId(request.getRequesterId());
        meeting.setRecipientId(request.getRecipientId());
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        meeting.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        meeting.setNotes(request.getNotes());
        
        Meeting createdMeeting = meetingService.createMeeting(meeting);
        return ResponseEntity.ok(createdMeeting);
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<Meeting> getMeetingById(@PathVariable Long meetingId) {
        Optional<Meeting> meeting = meetingService.getMeetingById(meetingId);
        return meeting.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/class/{classCode}/upcoming")
    public ResponseEntity<List<Meeting>> getUpcomingMeetings(@PathVariable String classCode) {
        List<Meeting> meetings = meetingService.getUpcomingMeetings(classCode);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/class/{classCode}")
    public ResponseEntity<List<Meeting>> getMeetingsByClassCode(@PathVariable String classCode) {
        List<Meeting> meetings = meetingService.getMeetingsByClassCode(classCode);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<Meeting>> getMeetingsByRequesterId(@PathVariable String requesterId) {
        List<Meeting> meetings = meetingService.getMeetingsByRequesterId(requesterId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Meeting>> getMeetingsByRecipientId(@PathVariable String recipientId) {
        List<Meeting> meetings = meetingService.getMeetingsByRecipientId(recipientId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/recipient/{recipientId}/pending")
    public ResponseEntity<List<Meeting>> getPendingMeetings(@PathVariable String recipientId) {
        List<Meeting> meetings = meetingService.getPendingMeetings(recipientId);
        return ResponseEntity.ok(meetings);
    }

    @PutMapping("/{meetingId}")
    public ResponseEntity<Meeting> updateMeeting(@PathVariable Long meetingId, @RequestBody MeetingRequest request) {
        Meeting updatedMeeting = new Meeting();
        updatedMeeting.setClassCode(request.getClassCode());
        updatedMeeting.setQuarter(request.getQuarter());
        updatedMeeting.setYear(request.getYear());
        updatedMeeting.setMeetingDate(request.getMeetingDate());
        updatedMeeting.setStartTime(request.getStartTime());
        updatedMeeting.setEndTime(request.getEndTime());
        updatedMeeting.setStatus(request.getStatus());
        updatedMeeting.setNotes(request.getNotes());
        
        Meeting result = meetingService.updateMeeting(meetingId, updatedMeeting);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long meetingId) {
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<Meeting>> createRecurringMeetings(@RequestBody RecurringMeetingRequest request) {
        List<Meeting> createdMeetings = meetingService.createRecurringMeetings(request);
        return ResponseEntity.ok(createdMeetings);
    }
}