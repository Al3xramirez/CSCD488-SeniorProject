package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "syllabus")
public class Syllabus {

    @EmbeddedId
    private CourseId id;

    @MapsId
    @OneToOne
    @JoinColumns({
        @JoinColumn(name = "ClassCode"),
        @JoinColumn(name = "Quarter"),
        @JoinColumn(name = "Year")
    })
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String gradingScale;

    @Column(columnDefinition = "TEXT")
    private String attendancePolicy;

    @Column(columnDefinition = "TEXT")
    private String latePolicy;

    @Column(columnDefinition = "TEXT")
    private String examInfo;
}