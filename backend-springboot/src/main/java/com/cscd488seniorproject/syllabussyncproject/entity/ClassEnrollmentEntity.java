package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollsrelation")
public class ClassEnrollmentEntity {

    @EmbeddedId
    public ClassEnrollmentId id;

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

    // Constructors
    public ClassEnrollmentEntity() {}

    public ClassEnrollmentEntity(ClassEnrollmentId id) {
        this.id = id;
    }

    // Getters and Setters
    public ClassEnrollmentId getId() {
        return id;
    }

    public void setId(ClassEnrollmentId id) {
        this.id = id;
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

    // Convenience getter methods
    public String getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public String getClassCode() {
        return id != null ? id.getClassCode() : null;
    }

    public String getQuarter() {
        return id != null ? id.getQuarter() : null;
    }

    public Integer getYear() {
        return id != null ? id.getYear() : null;
    }
}
