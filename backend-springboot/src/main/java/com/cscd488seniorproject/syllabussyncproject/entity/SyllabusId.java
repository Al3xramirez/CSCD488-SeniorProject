package com.cscd488seniorproject.syllabussyncproject.entity;

import java.io.Serializable;
import java.util.Objects;

public class SyllabusId implements Serializable {

    private String classCode;
    private String quarter;
    private String year;

    public SyllabusId() {
    }

    public SyllabusId(String classCode, String quarter, String year) {
        this.classCode = classCode;
        this.quarter = quarter;
        this.year = year;
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
        SyllabusId syllabusId = (SyllabusId) o;
        return Objects.equals(classCode, syllabusId.classCode) &&
                Objects.equals(quarter, syllabusId.quarter) &&
                Objects.equals(year, syllabusId.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classCode, quarter, year);
    }
}
