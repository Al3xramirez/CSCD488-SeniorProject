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

    Optional<CourseEntity> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    Optional<CourseEntity> findByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);

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

    List<CourseEntity> findByClassCode(String classCode);

    List<CourseEntity> findByQuarterAndYear(String quarter, Integer year);

    @Query("select c from CourseEntity c, TARelationEntity t " +
        "where t.userId = :userId " +
        "and t.classCode = c.classCode and t.quarter = c.quarter and t.year = c.year")
    List<CourseEntity> findCoursesTAedBy(@Param("userId") String userId);
}
