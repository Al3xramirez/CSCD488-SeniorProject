package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachesRelationRepository extends JpaRepository<TeachesRelationEntity, TeachesRelationId> {
    
    // Custom query to check if a teaching relation exists for a specific user and course
    boolean existsByUserIdAndClassCodeAndQuarterAndYear(String userId, String classCode, String quarter, String year);

    // Custom query to find all teaching relations for a specific user
    List<TeachesRelationEntity> findAllByUserId(String userId);

    // Delete all teaching relations for a specific course
    long deleteAllByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);
}
