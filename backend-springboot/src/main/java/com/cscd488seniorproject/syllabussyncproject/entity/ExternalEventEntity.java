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
            columnNames = {"SubscriptionID", "IcalUid", "RecurrenceId"}
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
    @Column(name = "ExternalEventID")
    private Long externalEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubscriptionID", nullable = false)
    private CalendarSubscriptionEntity subscription;

    @Column(name = "IcalUid", length = 255, nullable = false)
    private String icalUid;

    @Column(name = "RecurrenceId", length = 255)
    private String recurrenceId;

    @Column(name = "Summary", length = 255)
    private String summary;

    @Column(name = "Location", length = 255)
    private String location;

    @Column(name = "Description", columnDefinition = "text")
    private String description;

    @Column(name = "StartAt", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "EndAt", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "AllDay", nullable = false)
    private Boolean allDay;

    @Column(name = "IsCancelled", nullable = false)
    private Boolean isCancelled;

    @Column(name = "UpdatedFromFeedAt", nullable = false)
    private LocalDateTime updatedFromFeedAt;
}

