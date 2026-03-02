package com.cscd488seniorproject.syllabussyncproject.service;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.DateEnd;
import biweekly.property.DateStart;
import biweekly.property.Summary;
import com.cscd488seniorproject.syllabussyncproject.entity.CalendarEventEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.CalendarEventRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CanvasCalendarSyncService {

    private final UserAccountRepository userAccountRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CanvasFeedCryptoService cryptoService;
    private final HttpClient httpClient;

    public CanvasCalendarSyncService(UserAccountRepository userAccountRepository,
                                     CalendarEventRepository calendarEventRepository,
                                     CanvasFeedCryptoService cryptoService) {
        this.userAccountRepository = userAccountRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.cryptoService = cryptoService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Transactional
    public int syncForUser(UserAccountEntity user) {
        String encryptedUrl = user.getCanvasIcalUrlEncrypted();
        if (encryptedUrl == null || encryptedUrl.isBlank()) {
            return 0;
        }

        String feedUrl = cryptoService.decrypt(encryptedUrl);
        String icsContent = downloadIcs(feedUrl);
        ICalendar calendar = Biweekly.parse(icsContent).first();
        if (calendar == null) {
            return 0;
        }

        int upserted = 0;
        for (VEvent event : calendar.getEvents()) {
            DateStart dateStart = event.getDateStart();
            if (dateStart == null || dateStart.getValue() == null) {
                continue;
            }

            Instant startAt = toInstant(dateStart.getValue());
            Instant endAt = resolveEnd(event, startAt);
            String title = resolveTitle(event);
            String location = event.getLocation() == null ? null : event.getLocation().getValue();
            String externalUid = resolveExternalUid(event, title, startAt, endAt);

            Optional<CalendarEventEntity> existing =
                calendarEventRepository.findByUserIdAndExternalUid(user.getUserId(), externalUid);

            CalendarEventEntity entity = existing.orElseGet(CalendarEventEntity::new);
            entity.setUserId(user.getUserId());
            entity.setExternalUid(externalUid);
            entity.setTitle(title);
            entity.setStartAt(startAt);
            entity.setEndAt(endAt);
            entity.setLocation(location);
            entity.setSource("CANVAS");

            calendarEventRepository.save(entity);
            upserted++;
        }

        user.setCanvasLastSyncedAt(Instant.now());
        userAccountRepository.save(user);
        return upserted;
    }

    @Scheduled(fixedDelayString = "${app.canvas.sync-interval-ms:1800000}")
    public void syncAllCanvasFeeds() {
        List<UserAccountEntity> users = userAccountRepository.findByCanvasIcalUrlEncryptedIsNotNull();
        for (UserAccountEntity user : users) {
            try {
                syncForUser(user);
            } catch (Exception ignored) {
                // Continue syncing other users even if one feed fails.
            }
        }
    }

    private String downloadIcs(String feedUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(feedUrl))
                .header("Accept", "text/calendar")
                .GET()
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Canvas feed request failed with status " + response.statusCode());
            }
            return response.body();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch Canvas iCal feed", ex);
        }
    }

    private static Instant toInstant(Date date) {
        return date.toInstant();
    }

    private static Instant resolveEnd(VEvent event, Instant startAt) {
        DateEnd dateEnd = event.getDateEnd();
        if (dateEnd == null || dateEnd.getValue() == null) {
            return startAt;
        }
        return toInstant(dateEnd.getValue());
    }

    private static String resolveTitle(VEvent event) {
        Summary summary = event.getSummary();
        if (summary == null || summary.getValue() == null || summary.getValue().isBlank()) {
            return "Untitled Canvas Event";
        }
        return summary.getValue().trim();
    }

    private static String resolveExternalUid(VEvent event, String title, Instant startAt, Instant endAt) {
        if (event.getUid() != null && event.getUid().getValue() != null && !event.getUid().getValue().isBlank()) {
            return event.getUid().getValue();
        }

        String raw = (title + "|" + startAt + "|" + endAt).toLowerCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            return raw;
        }
    }
}
