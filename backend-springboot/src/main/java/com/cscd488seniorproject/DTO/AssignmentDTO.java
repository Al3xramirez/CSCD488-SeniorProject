package com.cscd488seniorproject.syllabussync.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class AssignmentDTO {
    private Long assignmentID;
    private String classCode;
    private String quarter;
    private String year;
    private String title;
    private String description;
    private Instant dueDate;
    private BigDecimal points;
    private BigDecimal weight;
    private BigDecimal estimatedEffortHours;
    private String category;

    public Long getAssignmentID() { return assignmentID; }
    public void setAssignmentID(Long assignmentID) { this.assignmentID = assignmentID; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getQuarter() { return quarter; }
    public void setQuarter(String quarter) { this.quarter = quarter; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(BigDecimal estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}