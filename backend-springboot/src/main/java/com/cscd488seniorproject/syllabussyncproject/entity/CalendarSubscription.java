package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SubscriptionID")
    private Long subscriptionId;

    @Column(name = "UserID", length = 20, nullable = false)
    private String userId;

    @Column(name = "Provider", length = 20, nullable = false)
    private String provider;

    @Column(name = "IcsUrl", columnDefinition = "text", nullable = false)
    private String icsUrl;

    @Column(name = "IsEnabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "LastSyncAt")
    private LocalDateTime lastSyncAt;

    @Column(name = "LastStatus", length = 20)
    private String lastStatus;

    @Column(name = "LastError", columnDefinition = "text")
    private String lastError;

    @Column(name = "ETag", length = 255)
    private String eTag;

    @Column(name = "LastModified", length = 255)
    private String lastModified;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

