package com.cscd488seniorproject.syllabussyncproject.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/my")
    public ResponseEntity<List<NotificationEntity>> getMyNotifications(Authentication auth) {
        if (auth == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(auth.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        if (auth == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        NotificationEntity n = notificationRepository.findById(id).orElse(null);
        if (n == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if (!n.getRecipientEmail().equals(auth.getName())) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        n.setRead(true);
        notificationRepository.save(n);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        if (auth == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        List<NotificationEntity> unread = notificationRepository.findByRecipientEmailAndReadFalse(auth.getName());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
