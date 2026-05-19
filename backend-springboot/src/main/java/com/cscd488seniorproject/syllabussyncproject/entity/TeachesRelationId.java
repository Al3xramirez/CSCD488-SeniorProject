package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TeachesRelationId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String classCode;
    private String quarter;
    private Integer year;

    public TeachesRelationId() {}

    public TeachesRelationId(String userId, String classCode, String quarter, Integer year) {
        this.userId = userId;
        this.classCode = classCode;
        this.quarter = quarter;
        this.year = year;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeachesRelationId that = (TeachesRelationId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(classCode, that.classCode) &&
               Objects.equals(quarter, that.quarter) &&
               Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, classCode, quarter, year);
    }
}
