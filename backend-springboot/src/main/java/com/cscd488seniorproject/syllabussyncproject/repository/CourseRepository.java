package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<CourseEntity, CourseId> {

    // Custom query to find a course by its join code
    Optional<CourseEntity> findByJoinCode(String joinCode);

    // Custom query to check if a course with a specific join code exists
    boolean existsByJoinCode(String joinCode);

    // Custom query to find a course by its class code, quarter, and year
    Optional<CourseEntity> findByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);

    // Custom query to find courses taught by a specific user
    @Query("select c from CourseEntity c, TeachesRelationEntity t " +
	    "where t.userId = :userId " +
	    "and t.classCode = c.classCode and t.quarter = c.quarter and t.year = c.year")
    List<CourseEntity> findCoursesTaughtBy(@Param("userId") String userId);

    // Custom query to find courses enrolled by a specific user
    @Query("select c from CourseEntity c, EnrollRelationEntity e " +
	    "where e.userId = :userId " +
	    "and e.classCode = c.classCode and e.quarter = c.quarter and e.year = c.year")
    List<CourseEntity> findCoursesEnrolledBy(@Param("userId") String userId);
}
