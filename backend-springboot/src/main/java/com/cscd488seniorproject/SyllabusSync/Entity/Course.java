package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "course")
public class Course {

    @EmbeddedId
    private CourseId id;

}