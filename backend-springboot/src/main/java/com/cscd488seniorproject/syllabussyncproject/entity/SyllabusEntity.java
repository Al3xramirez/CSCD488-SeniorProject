package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "syllabuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabusid")
    private Long syllabusId;

    /** References CourseEntity.joinCode — unique per course. */
    @Column(name = "join_code", nullable = false, unique = true, length = 20)
    private String joinCode;

    /** userId of the professor who uploaded the syllabus. */
    @Column(name = "uploaded_by", nullable = false, length = 20)
    private String uploadedBy;

    /** JSON: { days: string[], startTime: string, endTime: string, location: string } */
    @Column(name = "class_meeting_times", columnDefinition = "TEXT")
    private String classMeetingTimes;

    @Column(name = "office_hours", columnDefinition = "TEXT")
    private String officeHours;

    /** JSON array: [{ letter: string, range: string }] */
    @Column(name = "grade_scale", columnDefinition = "TEXT")
    private String gradeScale;

    /** JSON array: [{ component: string, weight: string }] */
    @Column(name = "grade_breakdown", columnDefinition = "TEXT")
    private String gradeBreakdown;

    /** JSON array: string[] */
    @Column(name = "pass_conditions", columnDefinition = "TEXT")
    private String passConditions;

    /** JSON: { tracked: string, affectsGrade: string, details: string } */
    @Column(name = "attendance", columnDefinition = "TEXT")
    private String attendance;

    /** JSON array: [{ name: string, date: string }] */
    @Column(name = "due_dates", columnDefinition = "TEXT")
    private String dueDates;

    @Column(name = "late_work_policy", columnDefinition = "TEXT")
    private String lateWorkPolicy;

    @Column(name = "ai_policy", columnDefinition = "TEXT")
    private String aiPolicy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
