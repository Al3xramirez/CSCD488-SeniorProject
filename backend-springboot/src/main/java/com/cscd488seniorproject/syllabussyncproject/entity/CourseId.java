package com.cscd488seniorproject.syllabussyncproject.entity;

import java.io.Serializable;
import java.util.Objects;

public class CourseId implements Serializable {

    private String classCode;
    private String quarter;
    private String year;

    public CourseId() {
    }

    public CourseId(String classCode, String quarter, String year) {
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseId courseId = (CourseId) o;
        return Objects.equals(year, courseId.year) &&
                Objects.equals(classCode, courseId.classCode) &&
                Objects.equals(quarter, courseId.quarter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classCode, quarter, year);
    }
}