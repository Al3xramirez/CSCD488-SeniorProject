package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;
@Entity
@Table(name = "course")
@IdClass(CourseId.class)
public class CourseEntity {

    @Id
    @Column(name = "ClassCode")
    private String classCode;

    @Id
    @Column(name = "Quarter")
    private String quarter;

    @Id
    @Column(name = "Year")
    private String year;

    @Column(name = "Title")
    private String title;

    @Column (name = "JoinCode")
    private String joinCode;

    public CourseEntity() {
    }

    // Getters and Setters
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
    
}
