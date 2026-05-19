package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, CourseId> {

    // Custom query to find a course by its join code
    Optional<CourseEntity> findByJoinCode(String joinCode);

    // Custom query to check if a course with a specific join code exists
    boolean existsByJoinCode(String joinCode);

    // Custom query to find a course by its class code, quarter, and year
    Optional<CourseEntity> findByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);

    // Custom query to find courses taught by a specific user
    @Query("""
        SELECT c
        FROM CourseEntity c
        INNER JOIN TeachesRelationEntity t
        ON c.classCode = t.classCode 
        AND c.quarter = t.quarter 
        AND c.year = t.year
        WHERE t.userId = :userId
    """)
    List<CourseEntity> findCoursesTaughtBy(@Param("userId") String userId);

    // Custom query to find courses enrolled by a specific user
    @Query("""
        SELECT c
        FROM CourseEntity c
        INNER JOIN EnrollRelationEntity e
        ON c.classCode = e.id.classCode 
        AND c.quarter = e.id.quarter 
        AND c.year = e.id.year
        WHERE e.id.userId = :userId
    """)
    List<CourseEntity> findCoursesEnrolledBy(@Param("userId") String userId);

    // ✅ ADDITIONAL: Find all courses by class code
    List<CourseEntity> findByClassCode(String classCode);

    // ✅ ADDITIONAL: Find courses by quarter and year
    List<CourseEntity> findByQuarterAndYear(String quarter, Integer year);
}
