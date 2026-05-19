package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CourseId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String classCode;  // PRIMARY KEY
    private String quarter;
    private Integer year;      

    // Constructors
    public CourseId() {}

    public CourseId(String classCode, String quarter, Integer year) {
        this.classCode = classCode;
        this.quarter = quarter;
        this.year = year;
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseId courseId = (CourseId) o;
        return Objects.equals(classCode, courseId.classCode) &&
               Objects.equals(quarter, courseId.quarter) &&
               Objects.equals(year, courseId.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classCode, quarter, year);
    }
}