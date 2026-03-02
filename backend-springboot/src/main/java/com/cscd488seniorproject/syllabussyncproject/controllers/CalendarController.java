package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.dto.CalendarEventResponse;
import com.cscd488seniorproject.syllabussyncproject.dto.CanvasFeedRequest;
import com.cscd488seniorproject.syllabussyncproject.dto.CanvasFeedStatusResponse;
import com.cscd488seniorproject.syllabussyncproject.entity.CalendarEventEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.CalendarEventRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.service.CanvasCalendarSyncService;
import com.cscd488seniorproject.syllabussyncproject.service.CanvasFeedCryptoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final UserAccountRepository userAccountRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CanvasFeedCryptoService cryptoService;
    private final CanvasCalendarSyncService syncService;

    public CalendarController(UserAccountRepository userAccountRepository,
                              CalendarEventRepository calendarEventRepository,
                              CanvasFeedCryptoService cryptoService,
                              CanvasCalendarSyncService syncService) {
        this.userAccountRepository = userAccountRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.cryptoService = cryptoService;
        this.syncService = syncService;
    }

    @PutMapping("/canvas-feed")
    public ResponseEntity<?> updateCanvasFeed(@RequestBody CanvasFeedRequest request, Authentication authentication) {
        if (request == null || request.url == null || request.url.isBlank()) {
            return ResponseEntity.badRequest().body("Canvas iCal URL is required");
        }

        String normalizedUrl = request.url.trim();
        if (!isValidCanvasIcsUrl(normalizedUrl)) {
            return ResponseEntity.badRequest().body("Provide a valid https Canvas iCal feed URL (.ics)");
        }

        UserAccountEntity user = getCurrentUser(authentication);
        user.setCanvasIcalUrlEncrypted(cryptoService.encrypt(normalizedUrl));
        userAccountRepository.save(user);

        return ResponseEntity.ok("Canvas feed saved");
    }

    @GetMapping("/canvas-feed")
    public ResponseEntity<CanvasFeedStatusResponse> getCanvasFeedStatus(Authentication authentication) {
        UserAccountEntity user = getCurrentUser(authentication);
        CanvasFeedStatusResponse response = new CanvasFeedStatusResponse();
        response.hasFeed = user.getCanvasIcalUrlEncrypted() != null && !user.getCanvasIcalUrlEncrypted().isBlank();
        response.lastSyncedAt = user.getCanvasLastSyncedAt();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/canvas/sync")
    public ResponseEntity<?> syncCanvas(Authentication authentication) {
        UserAccountEntity user = getCurrentUser(authentication);
        int synced = syncService.syncForUser(user);
        return ResponseEntity.ok("Canvas sync complete. Upserted " + synced + " events.");
    }

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getEvents(
        Authentication authentication,
        @RequestParam("from")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam("to")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        UserAccountEntity user = getCurrentUser(authentication);
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }

        List<CalendarEventResponse> responses = calendarEventRepository
            .findByUserIdAndStartAtBetweenOrderByStartAtAsc(user.getUserId(), from, to)
            .stream()
            .map(CalendarController::toResponse)
            .toList();

        return ResponseEntity.ok(responses);
    }

    private static CalendarEventResponse toResponse(CalendarEventEntity entity) {
        CalendarEventResponse response = new CalendarEventResponse();
        response.eventId = entity.getEventId();
        response.title = entity.getTitle();
        response.startAt = entity.getStartAt();
        response.endAt = entity.getEndAt();
        response.location = entity.getLocation();
        response.source = entity.getSource();
        return response;
    }

    private UserAccountEntity getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication is required");
        }

        return userAccountRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private static boolean isValidCanvasIcsUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
                return false;
            }

            String path = uri.getPath();
            return path != null && path.toLowerCase().endsWith(".ics");
        } catch (Exception ex) {
            return false;
        }
    }
}
