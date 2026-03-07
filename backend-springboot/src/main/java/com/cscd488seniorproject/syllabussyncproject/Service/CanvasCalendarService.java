package com.cscd488seniorproject.syllabussyncproject.Service;

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
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
            if (existing.isEmpty() && key.recurrenceId.isEmpty()) {
                existing = externalEventRepository.findBySubscription_SubscriptionIdAndIcalUidAndRecurrenceId(
                    subscription.getSubscriptionId(),
                    key.icalUid,
                    null
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
        Uid uidProp = (Uid) component.getProperty(Property.UID);
        if (uidProp == null || uidProp.getValue() == null || uidProp.getValue().isBlank()) {
            return null;
        }

        String uid = uidProp.getValue().trim();
        String recurrenceId = "";
        RecurrenceId rid = (RecurrenceId) component.getProperty(Property.RECURRENCE_ID);
        if (rid != null && rid.getValue() != null && !rid.getValue().isBlank()) {
            recurrenceId = rid.getValue().trim();
        }

        DtStart dtStart = (DtStart) component.getProperty(Property.DTSTART);
        if (dtStart == null || dtStart.getDate() == null) {
            return null;
        }

        ParsedDateTime start = parseDateProperty(dtStart);
        ParsedDateTime end = parseEnd(component, start);

        Summary summaryProp = (Summary) component.getProperty(Property.SUMMARY);
        Location locationProp = (Location) component.getProperty(Property.LOCATION);
        Description descriptionProp = (Description) component.getProperty(Property.DESCRIPTION);

        Status status = (Status) component.getProperty(Property.STATUS);
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

    private ParsedDateTime parseEnd(Component component, ParsedDateTime start) {
        DtEnd dtEnd = (DtEnd) component.getProperty(Property.DTEND);
        if (dtEnd == null || dtEnd.getDate() == null) {
            if (start.allDay) {
                return new ParsedDateTime(start.value.plusDays(1), true);
            }
            return new ParsedDateTime(start.value, false);
        }
        ParsedDateTime end = parseDateProperty(dtEnd);
        if (start.allDay && end.value.isBefore(start.value.plusDays(1))) {
            return new ParsedDateTime(start.value.plusDays(1), true);
        }
        return end;
    }

    private ParsedDateTime parseDateProperty(net.fortuna.ical4j.model.property.DateProperty property) {
        Date date = property.getDate();
        if (date instanceof DateTime dt) {
            return new ParsedDateTime(toLocalDateTime(dt, property.getParameters()), false);
        }

        String raw = property.getValue();
        LocalDate localDate;
        if (raw != null && raw.length() == 8) {
            localDate = LocalDate.parse(raw, DateTimeFormatter.BASIC_ISO_DATE);
        } else {
            localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return new ParsedDateTime(localDate.atStartOfDay(), true);
    }

    private LocalDateTime toLocalDateTime(DateTime dateTime, ParameterList parameters) {
        if (dateTime.isUtc()) {
            return Instant.ofEpochMilli(dateTime.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
        }

        ZoneId zoneId = null;
        Parameter tzid = parameters == null ? null : parameters.getParameter(Parameter.TZID);
        if (tzid != null && tzid.getValue() != null && !tzid.getValue().isBlank()) {
            try {
                zoneId = ZoneId.of(tzid.getValue().trim());
            } catch (Exception ignored) {
                zoneId = null;
            }
        }

        if (zoneId == null && dateTime.getTimeZone() != null) {
            zoneId = dateTime.getTimeZone().toZoneId();
        }
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }

        return Instant.ofEpochMilli(dateTime.getTime()).atZone(zoneId).toLocalDateTime();
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
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? "" : trimmed;
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
