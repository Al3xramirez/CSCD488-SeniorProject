package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.controller.dto.CanvasSubscribeRequest;
import com.cscd488seniorproject.syllabussyncproject.controller.dto.ExternalEventResponse;
import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscription;
import com.cscd488seniorproject.syllabussyncproject.entity.ExternalEvent;
import com.cscd488seniorproject.syllabussyncproject.repository.CalendarSubscriptionRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.ExternalEventRepository;
import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CanvasCalendarService {

    public static final String PROVIDER_CANVAS = "canvas";
    private static final int MAX_ERROR_LEN = 8000;
    private static final DateTimeFormatter ICAL_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ICAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICAL_DATE_TIME_MIN = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
    private static final DateTimeFormatter ICAL_DATE_TIME_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");

    private final CalendarSubscriptionRepository subscriptionRepository;
    private final ExternalEventRepository externalEventRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    @Transactional
    public CalendarSubscription subscribe(CanvasSubscribeRequest request) {
        String userId = request == null || request.userId() == null ? "" : request.userId().trim();
        String icsUrl = request == null || request.icsUrl() == null ? "" : request.icsUrl().trim();
        if (userId.isBlank() || icsUrl.isBlank()) {
            throw new IllegalArgumentException("userId and icsUrl are required");
        }

        CalendarSubscription subscription = subscriptionRepository
            .findByUserIdAndProvider(userId, PROVIDER_CANVAS)
            .orElseGet(() -> CalendarSubscription.builder()
                .userId(userId)
                .provider(PROVIDER_CANVAS)
                .build());

        boolean urlChanged = subscription.getIcsUrl() != null && !subscription.getIcsUrl().equals(icsUrl);
        subscription.setIcsUrl(icsUrl);
        subscription.setIsEnabled(true);
        subscription.setLastError(null);
        subscription.setLastStatus(null);
        if (urlChanged) {
            subscription.setETag(null);
            subscription.setLastModified(null);
        }

        subscription = subscriptionRepository.save(subscription);
        syncSubscription(subscription.getSubscriptionId());
        return subscription;
    }

    @Transactional(readOnly = true)
    public List<ExternalEventResponse> getMergedEventsForUserWindow(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(30);
        LocalDateTime to = now.plusDays(180);
        return externalEventRepository.findMergedEnabledEventsForUserInWindow(userId, from, to).stream()
            .map(ExternalEventResponse::fromEntity)
            .toList();
    }

    @Transactional
    public void syncAllEnabled() {
        for (CalendarSubscription subscription : subscriptionRepository.findByIsEnabledTrue()) {
            syncSubscription(subscription.getSubscriptionId());
        }
    }

    @Transactional
    public void syncSubscription(Long subscriptionId) {
        CalendarSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!Boolean.TRUE.equals(subscription.getIsEnabled())) {
            return;
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, "text/calendar");

            if (subscription.getETag() != null && !subscription.getETag().isBlank()) {
                headers.setIfNoneMatch(subscription.getETag());
            }

            if (subscription.getLastModified() != null && !subscription.getLastModified().isBlank()) {
                parseRfc1123(subscription.getLastModified()).ifPresent(zdt ->
                    headers.setIfModifiedSince(zdt.toInstant().toEpochMilli())
                );
            }

            ResponseEntity<byte[]> response = restTemplate.exchange(
                subscription.getIcsUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
            );

            subscription.setLastSyncAt(LocalDateTime.now());

            if (response.getStatusCode().value() == 304) {
                subscription.setLastStatus("NOT_MODIFIED");
                subscription.setLastError(null);
                subscriptionRepository.save(subscription);
                return;
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                subscription.setLastStatus("ERROR");
                subscription.setLastError("HTTP " + response.getStatusCode().value());
                subscriptionRepository.save(subscription);
                return;
            }

            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                subscription.setLastStatus("ERROR");
                subscription.setLastError("Empty ICS response");
                subscriptionRepository.save(subscription);
                return;
            }

            String etag = response.getHeaders().getETag();
            String lastModified = response.getHeaders().getFirst(HttpHeaders.LAST_MODIFIED);
            if (etag != null && !etag.isBlank()) {
                subscription.setETag(etag);
            }
            if (lastModified != null && !lastModified.isBlank()) {
                subscription.setLastModified(lastModified);
            }

            Calendar calendar = parseIcs(body);
            boolean hadParseErrors = upsertFromCalendar(subscription, calendar);

            subscription.setLastStatus(hadParseErrors ? "PARTIAL" : "OK");
            subscription.setLastError(hadParseErrors ? "Some events could not be parsed" : null);
            subscriptionRepository.save(subscription);
        } catch (RestClientResponseException ex) {
            subscription.setLastSyncAt(LocalDateTime.now());
            subscription.setLastStatus("ERROR");
            subscription.setLastError(trimToLen("HTTP " + ex.getRawStatusCode() + ": " + safeMessage(ex.getResponseBodyAsString()), MAX_ERROR_LEN));
            subscriptionRepository.save(subscription);
        } catch (Exception ex) {
            subscription.setLastSyncAt(LocalDateTime.now());
            subscription.setLastStatus("ERROR");
            subscription.setLastError(trimToLen(safeMessage(ex.getMessage()), MAX_ERROR_LEN));
            subscriptionRepository.save(subscription);
        }
    }

    private Calendar parseIcs(byte[] bytes) throws Exception {
        CalendarBuilder builder = new CalendarBuilder();
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return builder.build(in);
        }
    }

    private boolean upsertFromCalendar(CalendarSubscription subscription, Calendar calendar) {
        Map<EventKey, ParsedEvent> parsedByKey = new HashMap<>();
        boolean hadParseErrors = false;

        for (Component component : calendar.getComponents(Component.VEVENT)) {
            try {
                ParsedEvent parsed = parseVEvent(component);
                if (parsed == null) {
                    continue;
                }
                parsedByKey.put(new EventKey(parsed.icalUid, parsed.recurrenceId), parsed);
            } catch (Exception ignored) {
                hadParseErrors = true;
            }
        }

        Set<EventKey> keysInFeed = new HashSet<>(parsedByKey.keySet());

        for (Map.Entry<EventKey, ParsedEvent> entry : parsedByKey.entrySet()) {
            EventKey key = entry.getKey();
            ParsedEvent incoming = entry.getValue();

            Optional<ExternalEvent> existing = externalEventRepository
                .findBySubscription_SubscriptionIdAndIcalUidAndRecurrenceId(
                    subscription.getSubscriptionId(),
                    key.icalUid,
                    key.recurrenceId
                );
            if (existing.isEmpty() && key.recurrenceId == null) {
                existing = externalEventRepository.findBySubscription_SubscriptionIdAndIcalUidAndRecurrenceId(
                    subscription.getSubscriptionId(),
                    key.icalUid,
                    ""
                );
            }

            ExternalEvent entity = existing.orElseGet(ExternalEvent::new);

            entity.setSubscription(subscription);
            entity.setIcalUid(key.icalUid);
            entity.setRecurrenceId(key.recurrenceId);
            entity.setSummary(trimToLen(incoming.summary, 255));
            entity.setLocation(trimToLen(incoming.location, 255));
            entity.setDescription(incoming.description);
            entity.setStartAt(incoming.startAt);
            entity.setEndAt(incoming.endAt);
            entity.setAllDay(incoming.allDay);
            entity.setIsCancelled(incoming.isCancelled);
            entity.setUpdatedFromFeedAt(LocalDateTime.now());
            externalEventRepository.save(entity);
        }

        for (ExternalEvent existing : externalEventRepository.findBySubscription_SubscriptionIdAndIsCancelledFalse(subscription.getSubscriptionId())) {
            EventKey key = new EventKey(existing.getIcalUid(), normalizeRecurrenceId(existing.getRecurrenceId()));
            if (!keysInFeed.contains(key)) {
                existing.setIsCancelled(true);
                existing.setUpdatedFromFeedAt(LocalDateTime.now());
                externalEventRepository.save(existing);
            }
        }

        return hadParseErrors;
    }

    private ParsedEvent parseVEvent(Component component) {
        Uid uidProp = getProperty(component, Property.UID, Uid.class);
        if (uidProp == null || uidProp.getValue() == null || uidProp.getValue().isBlank()) {
            return null;
        }

        String uid = uidProp.getValue().trim();

        String recurrenceId = null;
        RecurrenceId rid = getProperty(component, Property.RECURRENCE_ID, RecurrenceId.class);
        if (rid != null && rid.getValue() != null && !rid.getValue().isBlank()) {
            recurrenceId = rid.getValue().trim();
        }

        DtStart dtStart = getProperty(component, Property.DTSTART, DtStart.class);
        if (dtStart == null || dtStart.getValue() == null || dtStart.getValue().isBlank()) {
            return null;
        }

        String startTzid = getTzid(dtStart);
        ParsedDateTime start = parseIcalDateTime(dtStart.getValue(), startTzid);
        ParsedDateTime end = parseEnd(component, start, startTzid);

        Summary summaryProp = getProperty(component, Property.SUMMARY, Summary.class);
        Location locationProp = getProperty(component, Property.LOCATION, Location.class);
        Description descriptionProp = getProperty(component, Property.DESCRIPTION, Description.class);

        Status status = getProperty(component, Property.STATUS, Status.class);
        boolean isCancelled = status != null && status.getValue() != null
            && status.getValue().trim().equalsIgnoreCase("CANCELLED");

        String summary = summaryProp == null ? null : summaryProp.getValue();
        String location = locationProp == null ? null : locationProp.getValue();
        String description = descriptionProp == null ? null : descriptionProp.getValue();

        return new ParsedEvent(
            uid,
            normalizeRecurrenceId(recurrenceId),
            summary,
            location,
            description,
            start.value,
            end.value,
            start.allDay,
            isCancelled
        );
    }

    private ParsedDateTime parseEnd(Component component, ParsedDateTime start, String fallbackTzid) {
        DtEnd dtEnd = getProperty(component, Property.DTEND, DtEnd.class);
        if (dtEnd == null || dtEnd.getValue() == null || dtEnd.getValue().isBlank()) {
            if (start.allDay) {
                return new ParsedDateTime(start.value.plusDays(1), true);
            }
            return new ParsedDateTime(start.value, false);
        }
        String endTzid = getTzid(dtEnd);
        if (endTzid == null) {
            endTzid = fallbackTzid;
        }
        ParsedDateTime end = parseIcalDateTime(dtEnd.getValue(), endTzid);
        if (start.allDay && end.value.isBefore(start.value.plusDays(1))) {
            return new ParsedDateTime(start.value.plusDays(1), true);
        }
        return end;
    }

    private ParsedDateTime parseIcalDateTime(String value, String tzid) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) {
            throw new IllegalArgumentException("Invalid iCal date");
        }

        if (v.length() == 8 && !v.contains("T")) {
            LocalDate d = LocalDate.parse(v, ICAL_DATE);
            return new ParsedDateTime(d.atStartOfDay(), true);
        }

        if (v.endsWith("Z")) {
            OffsetDateTime odt = OffsetDateTime.parse(v, ICAL_DATE_TIME_UTC);
            LocalDateTime ldt = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            return new ParsedDateTime(ldt, false);
        }

        LocalDateTime ldt = parseLocalDateTime(v);
        if (tzid == null || tzid.isBlank()) {
            return new ParsedDateTime(ldt, false);
        }

        ZoneId zoneId = safeZoneId(tzid);
        ZonedDateTime zoned = ldt.atZone(zoneId).withZoneSameInstant(ZoneId.systemDefault());
        return new ParsedDateTime(zoned.toLocalDateTime(), false);
    }

    private LocalDateTime parseLocalDateTime(String v) {
        try {
            return LocalDateTime.parse(v, ICAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(v, ICAL_DATE_TIME_MIN);
        }
    }

    private ZoneId safeZoneId(String tzid) {
        try {
            return ZoneId.of(tzid.trim());
        } catch (Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private String getTzid(Property property) {
        if (property == null || property.getParameters() == null) {
            return null;
        }
        for (Parameter p : property.getParameters()) {
            try {
                if ("TZID".equalsIgnoreCase(p.getName())) {
                    String v = p.getValue();
                    return v == null || v.isBlank() ? null : v.trim();
                }
            } catch (Exception ignored) {
                // Parameter API differs across ical4j versions; ignore and treat as floating time.
            }
        }
        return null;
    }

    private <T extends Property> T getProperty(Component component, String name, Class<T> type) {
        return component.getProperty(name).map(type::cast).orElse(null);
    }

    private Optional<ZonedDateTime> parseRfc1123(String value) {
        try {
            return Optional.of(ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String normalizeRecurrenceId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String safeMessage(String msg) {
        if (msg == null) {
            return "Unknown error";
        }
        return msg.trim().isBlank() ? "Unknown error" : msg.trim();
    }

    private static String trimToLen(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private record ParsedDateTime(LocalDateTime value, boolean allDay) {}

    private record ParsedEvent(
        String icalUid,
        String recurrenceId,
        String summary,
        String location,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        boolean isCancelled
    ) {}

    private record EventKey(String icalUid, String recurrenceId) {
        EventKey {
            icalUid = icalUid == null ? "" : icalUid.trim();
            recurrenceId = normalizeRecurrenceId(recurrenceId);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof EventKey that)) return false;
            return icalUid.equals(that.icalUid) && Objects.equals(recurrenceId, that.recurrenceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(icalUid, recurrenceId);
        }
    }
}
