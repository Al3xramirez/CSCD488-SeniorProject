package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "meeting")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MeetingID")
    private Long meetingID;

    @Column(name = "ClassCode", length = 20)
    private String classCode;

    @Column(name = "Quarter", length = 10)
    private String quarter;

    @Column(name = "Year")
    private Integer year;

    @Column(name = "RequesterID", nullable = false)
    private String requesterID;

    @Column(name = "RecipientID", nullable = false)
    private String recipientID;

    @Column(name = "MeetingDate", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "StartTime", nullable = false)
    private LocalTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalTime endTime;

    @Column(name = "Status", nullable = false)
    private String status = "PENDING";

    @Column(name = "Notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    public Meeting() {}

    public Long getMeetingID() { return meetingID; }
    public void setMeetingID(Long meetingID) { this.meetingID = meetingID; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getRequesterID() { return requesterID; }
    public void setRequesterID(String requesterID) { this.requesterID = requesterID; }

    public String getRecipientID() { return recipientID; }
    public void setRecipientID(String recipientID) { this.recipientID = recipientID; }

    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}