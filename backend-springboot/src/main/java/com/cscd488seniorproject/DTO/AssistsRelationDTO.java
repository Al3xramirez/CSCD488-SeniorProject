package com.cscd488seniorproject.syllabussync.dto;

public class AssistsRelationDTO {
    private String userID;
    private String classCode;
    private String quarter;
    private String year;

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}