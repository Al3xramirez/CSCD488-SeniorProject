package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "calendar_event",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_calendar_event_user_uid", columnNames = {"UserID", "ExternalUid"})
    },
    indexes = {
        @Index(name = "idx_calendar_event_user_start", columnList = "UserID, StartAt"),
        @Index(name = "idx_calendar_event_source", columnList = "Source")
    }
)
public class CalendarEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EventID")
    private Long eventId;

    @Column(name = "UserID", length = 20, nullable = false)
    private String userId;

    @Column(name = "Title", length = 255, nullable = false)
    private String title;

    @Column(name = "StartAt", nullable = false)
    private Instant startAt;

    @Column(name = "EndAt")
    private Instant endAt;

    @Column(name = "Location", length = 255)
    private String location;

    @Column(name = "Source", length = 30, nullable = false)
    private String source;

    @Column(name = "ExternalUid", length = 255, nullable = false)
    private String externalUid;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalUid() {
        return externalUid;
    }

    public void setExternalUid(String externalUid) {
        this.externalUid = externalUid;
    }
}
