package com.cscd488seniorproject.syllabussyncproject.meeting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RecurringMeetingRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classCode;
    private String quarter;
    private Integer year;
    private List<String> daysOfWeek;
    private String notes;
    private String recipientId;

    public RecurringMeetingRequest() {}

    // Getters and Setters
    public String getNotes(){
        return notes;
    }
    public void setNotes(String notes){
        this.notes = notes;
    }
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public String getRecipientId() {        
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
}