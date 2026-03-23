package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.controller.dto.CanvasSubscribeRequest;
import com.cscd488seniorproject.syllabussyncproject.controller.dto.ExternalEventResponse;
import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscriptionEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.service.CanvasCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CanvasCalendarController {

    private final CanvasCalendarService canvasCalendarService;
    private final UserAccountRepository userAccountRepository;

    private String resolveUserId(Authentication auth) {
        String email = auth.getName();
        UserAccountEntity user = userAccountRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return user.getUserId();
    }

    @PostMapping("/canvas/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody CanvasSubscribeRequest request, Authentication auth) {
        try {
            String userId = resolveUserId(auth);
            CalendarSubscriptionEntity subscription = canvasCalendarService.subscribe(userId, request);
            canvasCalendarService.syncSubscription(subscription.getSubscriptionId());
            CalendarSubscriptionEntity updated = canvasCalendarService.getSubscription(subscription.getSubscriptionId());
            if (updated.getLastError() != null) {
                return ResponseEntity.internalServerError().body("Subscription saved but sync failed: " + updated.getLastError());
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> getMergedEventsForUserWindow(Authentication auth) {
        try {
            String userId = resolveUserId(auth);
            List<ExternalEventResponse> events = canvasCalendarService.getMergedEventsForUserWindow(userId);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }
}
