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


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "calendar_subscription")
public class CalendarSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SubscriptionID")
    private Long subscriptionId;

    @Column(name = "UserID", nullable = false)
    private String userId;

    @Column(name = "Provider", nullable = false)
    private String provider;

    @Column(name = "IcsUrl", nullable = false)
    private String icsUrl;

    @Column(name = "IsEnabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "LastSyncAt")
    private LocalDateTime lastSyncAt;

    @Column(name = "LastStatus")
    private String lastStatus;

    @Column(name = "LastError")
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "etag")
    private String eTag;

    @Column(name = "last_modified")
    private String lastModified;

}

