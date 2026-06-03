package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cscd488seniorproject.syllabussyncproject.emailMeetingNotifications.EmailService;
import com.cscd488seniorproject.syllabussyncproject.notification.NotificationService;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseId;
import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.CourseRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.EnrollRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TeachesRelationRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserAccountRepository userAccountRepository;


    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollRelationRepository enrollRelationRepository;

    @Autowired
    private TeachesRelationRepository teachesRelationRepository;

    @PostMapping("/meetings/create-meeting")
    public ResponseEntity<MeetingEntity> createMeeting(@RequestBody MeetingRequest request) {
        MeetingEntity meeting = new MeetingEntity();
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
        
        MeetingEntity createdMeeting = meetingService.createMeeting(meeting);
        try { notificationService.saveMeetingRequestNotification(createdMeeting); }
        catch (Exception e) { System.err.println("Notification failed on create: " + e.getMessage()); }
        return ResponseEntity.ok(createdMeeting);
    }

    @GetMapping("/meetings/{meetingId}")
    public ResponseEntity<MeetingEntity> getMeetingById(@PathVariable Long meetingId) {
        Optional<MeetingEntity> meeting = meetingService.getMeetingById(meetingId);
        return meeting.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/classes/{classCode}/upcoming")
    public ResponseEntity<List<MeetingEntity>> getUpcomingMeetings(@PathVariable String classCode) {
        List<MeetingEntity> meetings = meetingService.getUpcomingMeetings(classCode);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/classes/{classCode}/class-meetings")
    public ResponseEntity<List<MeetingEntity>> getClassWideMeetings(@PathVariable String classCode) {
        List<MeetingEntity> meetings = meetingService.getClassWideMeetings(classCode);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/classes/{classCode}")
    public ResponseEntity<List<MeetingEntity>> getMeetingsByClassCode(@PathVariable String classCode) {
        List<MeetingEntity> meetings = meetingService.getMeetingsByClassCode(classCode);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/meetings/requester/{requesterId}")
    public ResponseEntity<List<MeetingEntity>> getMeetingsByRequesterId(@PathVariable String requesterId) {
        List<MeetingEntity> meetings = meetingService.getMeetingsByRequesterId(requesterId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/meetings/recipient/{recipientId}")
    public ResponseEntity<List<MeetingEntity>> getMeetingsByRecipientId(@PathVariable String recipientId) {
        List<MeetingEntity> meetings = meetingService.getMeetingsByRecipientId(recipientId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/meetings/recipient/{recipientId}/pending")
    public ResponseEntity<List<MeetingEntity>> getPendingMeetings(@PathVariable String recipientId) {
        List<MeetingEntity> meetings = meetingService.getPendingMeetings(recipientId);
        return ResponseEntity.ok(meetings);
    }

    @PutMapping("/meetings/{meetingId}")
    public ResponseEntity<MeetingEntity> updateMeeting(@PathVariable Long meetingId, @RequestBody MeetingRequest request) {
        MeetingEntity updatedMeeting = new MeetingEntity();
        updatedMeeting.setClassCode(request.getClassCode());
        updatedMeeting.setQuarter(request.getQuarter());
        updatedMeeting.setYear(request.getYear());
        updatedMeeting.setMeetingDate(request.getMeetingDate());
        updatedMeeting.setStartTime(request.getStartTime());
        updatedMeeting.setEndTime(request.getEndTime());
        updatedMeeting.setStatus(request.getStatus());
        updatedMeeting.setNotes(request.getNotes());
        
        MeetingEntity result = meetingService.updateMeeting(meetingId, updatedMeeting);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/meetings/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long meetingId, Authentication auth) {
        MeetingEntity toDelete = meetingService.getMeetingById(meetingId).orElse(null);
        if (toDelete == null) return ResponseEntity.notFound().build();
        if (auth == null || !auth.getName().equals(toDelete.getRecipientId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        meetingService.getMeetingById(meetingId).ifPresent(meeting -> {
            try { emailService.sendMeetingStatusNotification(meeting, "DECLINED"); } catch (Exception e) { System.err.println("Email failed on decline: " + e.getMessage()); }
            try { notificationService.saveMeetingDeclinedNotification(meeting); } catch (Exception e) { System.err.println("Notification failed on decline: " + e.getMessage()); }
        });
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/meetings/{meetingId}/status")
    public ResponseEntity<MeetingEntity> updateMeetingStatus(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String status = body.get("status");
        if (status == null) return ResponseEntity.badRequest().build();
        MeetingEntity existing = meetingService.getMeetingById(meetingId).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (auth == null || !auth.getName().equals(existing.getRecipientId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        MeetingEntity updated = meetingService.updateMeetingStatus(meetingId, status);
        if (updated != null) {
            try { emailService.sendMeetingStatusNotification(updated, status); } catch (Exception e) { System.err.println("Email failed on status update: " + e.getMessage()); }
            try { notificationService.saveMeetingConfirmedNotification(updated); } catch (Exception e) { System.err.println("Notification failed on confirm: " + e.getMessage()); }
        }
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/meetings/recurring")
    public ResponseEntity<?> createRecurringMeetings(@RequestBody RecurringMeetingRequest request) {
        try {
            System.err.println("RECURRING_MEETINGS_REQUEST: classCode=" + request.getClassCode()
                + ", startDate=" + request.getStartDate()
                + ", endDate=" + request.getEndDate()
                + ", startTime=" + request.getStartTime()
                + ", endTime=" + request.getEndTime()
                + ", daysOfWeek=" + request.getDaysOfWeek()
                + ", notes=" + request.getNotes());
            List<MeetingEntity> createdMeetings = meetingService.createRecurringMeetings(request);
            System.err.println("RECURRING_MEETINGS_CREATED: " + createdMeetings.size() + " meetings");
            return ResponseEntity.ok(createdMeetings);
        } catch (Exception e) {
            System.err.println("RECURRING_MEETINGS_ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Meeting creation failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/meetings/my")
    public ResponseEntity<List<MeetingEntity>> getMyMeetings(Authentication auth) {
        try {
            if (auth == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            String email = auth.getName();

            Set<Long> seen = new HashSet<>();
            List<MeetingEntity> all = new ArrayList<>();

            // Personal meetings (requester or recipient = this user's email)
            for (MeetingEntity m : meetingService.getMeetingsByUserId(email)) {
                if (seen.add(m.getMeetingId())) all.add(m);
            }

            // Class-wide meetings for every class the user is enrolled in or teaches
            Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                String uid = userOpt.get().getUserId();
                Set<String> classCodes = new HashSet<>();
                enrollRelationRepository.findByUserId(uid).forEach(e -> {
                    if (e.getClassCode() != null) classCodes.add(e.getClassCode());
                });
                teachesRelationRepository.findAllByUserId(uid).forEach(t -> {
                    if (t.getClassCode() != null) classCodes.add(t.getClassCode());
                });
                for (String classCode : classCodes) {
                    for (MeetingEntity m : meetingService.getClassWideMeetings(classCode)) {
                        if (seen.add(m.getMeetingId())) all.add(m);
                    }
                }
            }

            all.sort((a, b) -> {
                if (a.getMeetingDate() == null) return 1;
                if (b.getMeetingDate() == null) return -1;
                int c = a.getMeetingDate().compareTo(b.getMeetingDate());
                if (c != 0) return c;
                if (a.getStartTime() == null) return 1;
                if (b.getStartTime() == null) return -1;
                return a.getStartTime().compareTo(b.getStartTime());
            });
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            System.err.println("MY_MEETINGS_ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/meetings/user/{userEmail}/date/{date}")
    public ResponseEntity<List<MeetingEntity>> getMeetingsForUserByDate(
        @PathVariable String userEmail,
        @PathVariable String date
    ) {
        List<MeetingEntity> meetings = meetingService.getMeetingsForUserByDate(userEmail, date);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/auth/current-user")
    public ResponseEntity<Map<String, String>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String currentUser = auth.getName();
        return ResponseEntity.ok(Map.of("currentUser", currentUser));
    }

    @GetMapping("/classes/user-classes")
    public ResponseEntity<List<Map<String, Object>>> getUserClasses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail = auth.getName(); // This is the email from auth
        System.out.println("DEBUG: Loading classes for userEmail: " + userEmail);
        
        // Find the user by email to get their UserID
        Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(userEmail);
        if (!userOpt.isPresent()) {
            System.out.println("DEBUG: User not found with email: " + userEmail);
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        String userId = userOpt.get().getUserId(); // Get the actual UserID
        System.out.println("DEBUG: Found userId: " + userId);
        
        List<EnrollRelationEntity> enrollments = enrollRelationRepository.findByUserId(userId);
        System.out.println("DEBUG: Found " + enrollments.size() + " enrollments");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (EnrollRelationEntity e : enrollments) {
            System.out.println("DEBUG: Enrollment - classCode: " + e.getClassCode());
            CourseEntity course = courseRepository.findById(
                new CourseId(e.getClassCode(), e.getQuarter(), e.getYear())
            ).orElse(null);
            
            Map<String, Object> map = new HashMap<>();
            map.put("classID", e.getClassCode());
            map.put("courseCode", e.getClassCode());
            map.put("className", course != null ? course.getClassName() : "Unknown");
            map.put("quarter", e.getQuarter());
            map.put("year", e.getYear());
            result.add(map);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/classes/{classCode}/members")
    public ResponseEntity<List<Map<String, Object>>> getClassMembers(@PathVariable String classCode) {
        List<Map<String, Object>> members = new ArrayList<>();
        Set<String> addedUsers = new HashSet<>();
        
        // Get all students enrolled in this class
        List<EnrollRelationEntity> enrollments = enrollRelationRepository.findByClassCode(classCode);
        System.out.println("DEBUG: Found " + enrollments.size() + " students in class " + classCode);
        
        for (EnrollRelationEntity e : enrollments) {
            String userId = e.getId().getUserId(); // Get UserID from embedded ID
            System.out.println("DEBUG: Student userId: " + userId);
            
            if (!addedUsers.contains(userId)) {
                Optional<UserAccountEntity> user = userAccountRepository.findById(userId);
                if (user.isPresent()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userID", userId);
                    map.put("email", user.get().getEmail());
                    map.put("name", user.get().getFirstName() + " " + user.get().getLastName());
                    map.put("role", "Student");
                    members.add(map);
                    addedUsers.add(userId);
                } else {
                    System.out.println("DEBUG: User not found for userId: " + userId);
                }
            }
        }
        
        // Get professors/teachers for this class
        List<TeachesRelationEntity> teaches = teachesRelationRepository.findByClassCode(classCode);
        System.out.println("DEBUG: Found " + teaches.size() + " professors in class " + classCode);
        
        for (TeachesRelationEntity t : teaches) {
            String userId = t.getUserId(); // Get UserID from embedded ID
            System.out.println("DEBUG: Professor userId: " + userId);
            
            if (!addedUsers.contains(userId)) {
                Optional<UserAccountEntity> user = userAccountRepository.findById(userId);
                if (user.isPresent()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userID", userId);
                    map.put("email", user.get().getEmail());
                    map.put("name", user.get().getFirstName() + " " + user.get().getLastName());
                    map.put("role", "Professor");
                    members.add(map);
                    addedUsers.add(userId);
                } else {
                    System.out.println("DEBUG: User not found for userId: " + userId);
                }
            }
        }
        
        System.out.println("DEBUG: Returning " + members.size() + " total members");
        return ResponseEntity.ok(members);
    }

    @GetMapping("/meetings")
    public ResponseEntity<List<Map<String, Object>>> getAllMeetings() {
        try {
            List<MeetingEntity> meetings = meetingService.getAllMeetings();
            System.out.println("DEBUG: Returning " + meetings.size() + " meetings");
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (MeetingEntity m : meetings) {
                Map<String, Object> map = new HashMap<>();
                map.put("meetingID", m.getMeetingId());
                map.put("requesterID", m.getRequesterId());
                map.put("recipientID", m.getRecipientId());
                map.put("meetingDate", m.getMeetingDate());
                map.put("startTime", m.getStartTime());
                map.put("endTime", m.getEndTime());
                map.put("classCode", m.getClassCode());
                map.put("notes", m.getNotes());
                map.put("status", m.getStatus());
                
                System.out.println("Meeting: " + m.getMeetingId() + 
                    " requester=" + m.getRequesterId() + 
                    " recipient=" + m.getRecipientId() + 
                    " date=" + m.getMeetingDate());
                
                result.add(map);
            }
            
            System.out.println("DEBUG: Returning " + result.size() + " formatted meetings");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error fetching all meetings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable String userId) {
        Optional<UserAccountEntity> user = userAccountRepository.findById(userId);
        if (user.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("userID", user.get().getUserId());
            result.put("email", user.get().getEmail());
            result.put("firstName", user.get().getFirstName());
            result.put("lastName", user.get().getLastName());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/users/by-email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        Optional<UserAccountEntity> user = userAccountRepository.findByEmail(email);
        if (user.isPresent()) {
            Map<String, Object> result = new HashMap<>();
            result.put("userID", user.get().getUserId());
            result.put("email", user.get().getEmail());
            result.put("firstName", user.get().getFirstName());
            result.put("lastName", user.get().getLastName());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
}