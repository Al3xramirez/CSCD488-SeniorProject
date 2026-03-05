package com.cscd488seniorproject.syllabussync.entity;

@Embeddable
public class CourseId implements Serializable {

    private String classCode;
    private String quarter;
    private String year;

    // REQUIRED for composite keys
    @Override
    public boolean equals(CourseId o) {
        if (this == o) return true;
        if (!(o instanceof CourseId)) return false;
        CourseId that = o;
        return Objects.equals(classCode, that.classCode) &&
               Objects.equals(quarter, that.quarter) &&
               Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classCode, quarter, year);
    }
}