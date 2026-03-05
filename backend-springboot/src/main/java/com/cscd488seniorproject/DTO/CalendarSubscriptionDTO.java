package com.cscd488seniorproject.syllabussync.dto;

import java.time.Instant;

public class CalendarSubscriptionDTO {
    private Long subscriptionID;
    private String userID;
    private String provider;
    private String icsUrl;
    private Boolean isEnabled;
    private Instant lastSyncAt;
    private String lastStatus;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getSubscriptionID() { return subscriptionID; }
    public void setSubscriptionID(Long subscriptionID) { this.subscriptionID = subscriptionID; }
    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getIcsUrl() { return icsUrl; }
    public void setIcsUrl(String icsUrl) { this.icsUrl = icsUrl; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}