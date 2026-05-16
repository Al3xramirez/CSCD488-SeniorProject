package com.cscd488seniorproject.syllabussync.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class Meetings {
    private int meetingID;
    private String classCode;
    private String quarter;
    private int year;
    private int requesterID;
    private int recipientID;
    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public int getMeetingID() {
        return meetingID;
    }
    public void setMeetingID(int meetingID) {
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
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public int getRequesterID() {
        return requesterID;
    }
    public void setRequesterID(int requesterID) {
        this.requesterID = requesterID;
    }
    public int getRecipientID() {
        return recipientID;
    }
    public void setRecipientID(int recipientID) {
        this.recipientID = recipientID;
    }
    public String getMeetingDate() {
        return meetingDate;
    }
    public void setMeetingDate(String meetingDate) {
        this.meetingDate = meetingDate;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}