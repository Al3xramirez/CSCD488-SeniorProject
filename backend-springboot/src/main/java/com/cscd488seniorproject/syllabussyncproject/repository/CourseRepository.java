package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.Course;
import com.cscd488seniorproject.syllabussync.entity.CourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, CourseId> {
}