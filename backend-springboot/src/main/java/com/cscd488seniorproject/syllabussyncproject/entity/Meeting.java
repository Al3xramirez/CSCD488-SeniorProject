package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "meeting")
public class Meeting {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "MeetingID")
private Long meetingID;

@Column(name = "ClassCode")
private String classCode;

@Column(name = "Quarter")
private String quarter;

@Column(name = "Year")
private Integer year;

@Column(name = "RequesterID")
private Long requesterID;

@Column(name = "RecipientID")
private Long recipientID;

@Column(name = "MeetingDate")
private LocalDate meetingDate;

@Column(name = "StartTime")
private LocalTime startTime;

@Column(name = "EndTime")
private LocalTime endTime;

@Column(name = "Status")
private String status;

public Meeting() {}

public Long getMeetingID() {
    return meetingID;
}

public void setMeetingID(Long meetingID) {
    this.meetingID = meetingID;
}

public String getClassCode() {
    return classCode;
}

public void setClassCode(String classCode) {
    this.classCode = classCode;
}

public String getQuarter() {
    return quarter;
}

public void setQuarter(String quarter) {
    this.quarter = quarter;
}

public Integer getYear() {
    return year;
}

public void setYear(Integer year) {
    this.year = year;
}

public Long getRequesterID() {
    return requesterID;
}

public void setRequesterID(Long requesterID) {
    this.requesterID = requesterID;
}

public Long getRecipientID() {
    return recipientID;
}

public void setRecipientID(Long recipientID) {
    this.recipientID = recipientID;
}

public LocalDate getMeetingDate() {
    return meetingDate;
}

public void setMeetingDate(LocalDate meetingDate) {
    this.meetingDate = meetingDate;
}

public LocalTime getStartTime() {
    return startTime;
}

public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
}

public LocalTime getEndTime() {
    return endTime;
}

public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
}

public String getStatus() {
    return status;
}

public void setStatus(String status) {
    this.status = status;
}

}
