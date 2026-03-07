package com.cscd488seniorproject.syllabussyncproject.controller.dto;

import com.cscd488seniorproject.syllabussyncproject.entity.ExternalEvent;

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
    public static ExternalEventResponse fromEntity(ExternalEvent event) {
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

