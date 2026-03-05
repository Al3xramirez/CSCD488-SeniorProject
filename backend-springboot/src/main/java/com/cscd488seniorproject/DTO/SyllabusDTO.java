package com.cscd488seniorproject.syllabussync.dto;

public class SyllabusDTO {
    private String classCode;
    private String quarter;
    private String year;
    private String gradingScale;
    private String attendancePolicy;
    private String latePolicy;
    private String examInfo;

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getGradingScale() { return gradingScale; }
    public void setGradingScale(String gradingScale) { this.gradingScale = gradingScale; }
    public String getAttendancePolicy() { return attendancePolicy; }
    public void setAttendancePolicy(String attendancePolicy) { this.attendancePolicy = attendancePolicy; }
    public String getLatePolicy() { return latePolicy; }
    public void setLatePolicy(String latePolicy) { this.latePolicy = latePolicy; }
    public String getExamInfo() { return examInfo; }
    public void setExamInfo(String examInfo) { this.examInfo = examInfo; }
}