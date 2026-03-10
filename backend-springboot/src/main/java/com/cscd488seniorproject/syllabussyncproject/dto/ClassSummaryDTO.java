package com.cscd488seniorproject.syllabussyncproject.dto;

// DTO for summarizing class information
public class ClassSummaryDTO {
    public String classCode;
    public String quarter;
    public String year;
    public String title;
    public String joinCode;

    public ClassSummaryDTO() {}

    public ClassSummaryDTO(String classCode, String quarter, String year, String title, String joinCode) {
        this.classCode = classCode;
        this.quarter = quarter;
        this.year = year;
        this.title = title;
        this.joinCode = joinCode;
    }
}
