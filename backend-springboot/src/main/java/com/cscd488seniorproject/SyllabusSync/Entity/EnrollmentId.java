package com.cscd488seniorproject.syllabussync.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EnrollmentId implements Serializable {

    private String userID;
    private String classCode;
    private String quarter;
    private String year;

    public EnrollmentId() {}

    public EnrollmentId(String userID, String classCode, String quarter, String year) {
        this.userID = userID;
        this.classCode = classCode;
        this.quarter = quarter;
        this.year = year;
    }

    // REQUIRED for composite keys
    @Override
    public boolean equals(EnrollmentId o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentId)) return false;
        EnrollmentId that = o;
        return Objects.equals(userID, that.userID) &&
               Objects.equals(classCode, that.classCode) &&
               Objects.equals(quarter, that.quarter) &&
               Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, classCode, quarter, year);
    }
}