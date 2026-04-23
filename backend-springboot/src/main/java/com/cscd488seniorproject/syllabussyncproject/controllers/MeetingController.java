package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.entity.Meeting;
import com.cscd488seniorproject.syllabussyncproject.repository.MeetingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
public class MeetingController {

    private final MeetingRepository meetingRepository;

    public MeetingController(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @PostMapping("/create-meeting")
    public ResponseEntity<?> createMeeting(@RequestBody Map<String, String> payload, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No authenticated user"));
            }

            String requesterID = auth.getName();
            String recipientID = payload.get("recipientID");
            String date = payload.get("date");
            String start = payload.get("start");
            String end = payload.get("end");
            String notes = payload.get("notes");

            if (recipientID == null || recipientID.isBlank() ||
                date == null || date.isBlank() ||
                start == null || start.isBlank() ||
                end == null || end.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing required fields"));
            }

            LocalTime startTime = LocalTime.parse(start);
            LocalTime endTime = LocalTime.parse(end);
            if (!startTime.isBefore(endTime)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "End time must be after start time"));
            }

            // Check for conflicts in recipient's schedule
            LocalDate meetingDate = LocalDate.parse(date);
            List<Meeting> recipientMeetings = meetingRepository.findByRecipientIDAndMeetingDate(recipientID, meetingDate);
            boolean hasConflict = recipientMeetings.stream().anyMatch(m ->
                !(endTime.isBefore(m.getStartTime()) || startTime.isAfter(m.getEndTime().minusSeconds(1)))
            );
            if (hasConflict) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "The recipient has a conflicting meeting at this time."));
            }

            Meeting meeting = new Meeting();
            meeting.setRequesterID(requesterID);
            meeting.setRecipientID(recipientID);
            meeting.setMeetingDate(meetingDate);
            meeting.setStartTime(startTime);
            meeting.setEndTime(endTime);
            meeting.setNotes(notes != null ? notes : "");

            Meeting savedMeeting = meetingRepository.save(meeting);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Meeting created successfully", "meeting", savedMeeting));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create meeting: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllMeetings(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            Authentication auth) {
        try {
            // Only return meetings for authenticated user
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No authenticated user"));
            }

            String userID = auth.getName();
            List<Meeting> meetings;

            if (start != null && end != null) {
                LocalDate startDate = LocalDate.parse(start.split("T")[0]);
                LocalDate endDate = LocalDate.parse(end.split("T")[0]);
                meetings = meetingRepository.findByUserIDAndDateBetween(userID, startDate, endDate);
            } else {
                meetings = meetingRepository.findByUserID(userID);
            }

            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch meetings"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMeetingById(@PathVariable Long id) {
        try {
            Optional<Meeting> meeting = meetingRepository.findById(id);
            if (meeting.isPresent()) {
                return ResponseEntity.ok(meeting.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Meeting not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch meeting"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeeting(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Optional<Meeting> existing = meetingRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Meeting not found"));
            }

            Meeting meeting = existing.get();

            if (payload.containsKey("recipientID") && payload.get("recipientID") != null) {
                meeting.setRecipientID(payload.get("recipientID"));
            }
            if (payload.containsKey("date") && payload.get("date") != null) {
                meeting.setMeetingDate(LocalDate.parse(payload.get("date")));
            }
            if (payload.containsKey("start") && payload.get("start") != null) {
                meeting.setStartTime(LocalTime.parse(payload.get("start")));
            }
            if (payload.containsKey("end") && payload.get("end") != null) {
                meeting.setEndTime(LocalTime.parse(payload.get("end")));
            }
            if (payload.containsKey("notes")) {
                meeting.setNotes(payload.get("notes"));
            }

            Meeting updated = meetingRepository.save(meeting);
            return ResponseEntity.ok(Map.of("message", "Meeting updated successfully", "meeting", updated));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update meeting"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Long id) {
        try {
            if (!meetingRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Meeting not found"));
            }

            meetingRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Meeting deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete meeting"));
        }
    }
}