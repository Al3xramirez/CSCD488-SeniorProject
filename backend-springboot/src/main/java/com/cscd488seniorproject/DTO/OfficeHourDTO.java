package com.cscd488seniorproject.syllabussync.dto;

import java.time.LocalTime;

public class OfficeHourDTO {
    private Long officeHourID;
    private String userID;
    private String classCode;
    private String quarter;
    private String year;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public Long getOfficeHourID() { return officeHourID; }
    public void setOfficeHourID(Long officeHourID) { this.officeHourID = officeHourID; }
    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}