package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "external_event",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_external_event_subscription_uid_recur",
            columnNames = {"subscriptionid", "ical_uid", "recurrence_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_eventid")
    private Long externalEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriptionid", nullable = false)
    private CalendarSubscriptionEntity subscription;

    @Column(name = "ical_uid", length = 255, nullable = false)
    private String icalUid;

    @Column(name = "recurrence_id", length = 255)
    private String recurrenceId;

    @Column(name = "summary", length = 255)
    private String summary;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private Boolean allDay;

    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled;

    @Column(name = "updated_from_feed_at", nullable = false)
    private LocalDateTime updatedFromFeedAt;
}

