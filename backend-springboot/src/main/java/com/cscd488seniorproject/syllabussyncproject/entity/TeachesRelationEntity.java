package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teachesrelation")
@IdClass(TeachesRelationId.class)
public class TeachesRelationEntity {

    @Id
    @Column(name = "UserID")
    private String userId;

    @Id
    @Column(name = "ClassCode")
    private String classCode;

    @Id
    @Column(name = "Quarter")
    private String quarter;

    @Id
    @Column(name = "Year")
    private Integer year;


    public TeachesRelationEntity() {
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

    public void setYear(Integer year2) {
        this.year = year2;
    }

}
