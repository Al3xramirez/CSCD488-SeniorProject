package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollsrelation")
public class EnrollRelationEntity {

    @EmbeddedId
    private EnrollRelationId id;

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
    public EnrollRelationEntity() {}

    public EnrollRelationEntity(EnrollRelationId id) {
        this.id = id;
    }

    // Getters and Setters
    public EnrollRelationId getId() {
        return id;
    }

    public void setId(EnrollRelationId id) {
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

    public void setUserId(String userId) {
        if (id == null) {
            id = new EnrollRelationId();
        }
        id.setUserId(userId);
    }

    public void setClassCode(String classCode) {
        if (id == null) {
            id = new EnrollRelationId();
        }
        id.setClassCode(classCode);
    }

    public void setQuarter(String quarter) {
        if (id == null) {
            id = new EnrollRelationId();
        }
        id.setQuarter(quarter);
    }

    public void setYear(Integer year) {
        if (id == null) {
            id = new EnrollRelationId();
        }
        id.setYear(year);
    }
}
