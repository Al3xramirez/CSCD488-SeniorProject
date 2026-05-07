package com.cscd488seniorproject.syllabussyncproject.meeting;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MeetingRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayOfWeek> daysOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    private Long classId;

    public MeetingRequest() {}

    public MeetingRequest(
        LocalDate startDate, LocalDate endDate,
        List<DayOfWeek> daysOfWeek,
        LocalTime startTime, LocalTime endTime,                  
        Long classId) {
            
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysOfWeek = daysOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.classId = classId;
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

    public List<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public boolean isValid() {
        return startDate != null &&
               endDate != null &&
               !endDate.isBefore(startDate) &&
               daysOfWeek != null &&
               !daysOfWeek.isEmpty() &&
               startTime != null &&
               endTime != null &&
               startTime.isBefore(endTime) &&
               classId != null;
    }
}