package com.cscd488seniorproject.syllabussync.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public class ExternalEventDTO {
    private Long externalEventID;
    private Long subscriptionID;
    private String icalUid;
    private String recurrenceId;
    private String summary;
    private String location;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean allDay;
    private Boolean isCancelled;
    private Instant updatedFromFeedAt;

    public Long getExternalEventID() { return externalEventID; }
    public void setExternalEventID(Long externalEventID) { this.externalEventID = externalEventID; }
    public Long getSubscriptionID() { return subscriptionID; }
    public void setSubscriptionID(Long subscriptionID) { this.subscriptionID = subscriptionID; }
    public String getIcalUid() { return icalUid; }
    public void setIcalUid(String icalUid) { this.icalUid = icalUid; }
    public String getRecurrenceId() { return recurrenceId; }
    public void setRecurrenceId(String recurrenceId) { this.recurrenceId = recurrenceId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Boolean getAllDay() { return allDay; }
    public void setAllDay(Boolean allDay) { this.allDay = allDay; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean isCancelled) { this.isCancelled = isCancelled; }
    public Instant getUpdatedFromFeedAt() { return updatedFromFeedAt; }
    public void setUpdatedFromFeedAt(Instant updatedFromFeedAt) { this.updatedFromFeedAt = updatedFromFeedAt; }
}