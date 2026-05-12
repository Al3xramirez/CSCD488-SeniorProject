package com.cscd488seniorproject.syllabussyncproject.dto;

import com.cscd488seniorproject.syllabussyncproject.entity.ExternalEventEntity;

import java.time.LocalDateTime;

public record ExternalEventResponse(
    Long subscriptionId,
    String provider,
    String icalUid,
    String recurrenceId,
    String summary,
    String location,
    String description,
    LocalDateTime startAt,
    LocalDateTime endAt,
    boolean allDay,
    boolean isCancelled
) {
    public static ExternalEventResponse fromEntity(ExternalEventEntity event) {
        return new ExternalEventResponse(
            event.getSubscription().getSubscriptionId(),
            event.getSubscription().getProvider(),
            event.getIcalUid(),
            event.getRecurrenceId(),
            event.getSummary(),
            event.getLocation(),
            event.getDescription(),
            event.getStartAt(),
            event.getEndAt(),
            Boolean.TRUE.equals(event.getAllDay()),
            Boolean.TRUE.equals(event.getIsCancelled())
        );
    }
}

