package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping("/recurring")
    public ResponseEntity<String> createRecurring(@RequestBody MeetingRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().body("Invalid meeting request");
        }

        meetingService.createRecurringMeetings(request);
        return ResponseEntity.ok("Meetings created");
    }
}