package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourse_ClassCodeAndCourse_QuarterAndCourse_Year(
        String classCode, String quarter, String year);

    List<Assignment> findByDueDateBefore(Instant date);

    List<Assignment> findByCategory(String category);
}