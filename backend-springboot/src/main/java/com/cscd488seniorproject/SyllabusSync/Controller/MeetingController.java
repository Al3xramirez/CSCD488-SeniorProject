package com.cscd488seniorproject.syllabussync.meetingscontroller;


@RestController
@RequestMapping("/api/meetings")
public class MeetingsController {

    @PostMapping("/create")
    public ResponseEntity<String> createMeeting(@RequestBody Meetings meeting) {
        System.out.println("Received meeting: " + meeting.getClassCode() + ", " + meeting.getMeetingDate());
        return ResponseEntity.ok("Meeting created successfully!");
    }

    @GetMapping("/read{meetingID}")
    public ResponseEntity<String> readMeeting(@RequestBody User user) {
        System.out.println("Received meeting: " + meeting.getClassCode() + ", " + meeting.getMeetingDate());
        return ResponseEntity.ok("Meeting read successfully!");
    }

    @DeleteMapping("/delete") // call this after update metetnig is devlined, and notify the requester that the meeting was declined/deleted
    public ResponseEntity<String> deleteMeeting(@RequestBody Meetings meetingID) {
        System.out.println("Received meeting: " + meeting.getClassCode() + ", " + meeting.getMeetingDate());
        return ResponseEntity.ok("Meeting deleted successfully!");
    }
    
                                    //use this to update the meeting status ie meeting declined meeting accepted
    @PatchMapping("/edit")          // call this after update requested // can edit meeting details only before response is given // maybe we dont need this we could just delete and create a new meeting, strech goal?
    public ResponseEntity<String> editMeeting(@RequestBody Meetings meeting) { //will need to be a meeting instance with eth same id of a existing
        System.out.println("Received meeting: " + meeting.getClassCode() + ", " + meeting.getMeetingDate());
        return ResponseEntity.ok("Meeting updated successfully!");
    }

}