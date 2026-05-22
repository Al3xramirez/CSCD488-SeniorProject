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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private UserAccountEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "ClassCode", insertable = false, updatable = false),
        @JoinColumn(name = "Quarter", insertable = false, updatable = false),
        @JoinColumn(name = "Year", insertable = false, updatable = false)
    })
    private CourseEntity courseEntity;

    public TeachesRelationEntity() {
    }

    public TeachesRelationEntity(String userId, String classCode, String quarter, Integer year) {
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

    public UserAccountEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserAccountEntity userEntity) {
        this.userEntity = userEntity;
    }

    public CourseEntity getCourseEntity() {
        return courseEntity;
    }

    public void setCourseEntity(CourseEntity courseEntity) {
        this.courseEntity = courseEntity;
    }
}
