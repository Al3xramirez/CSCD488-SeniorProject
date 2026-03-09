package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollRelationRepository extends JpaRepository<EnrollRelationEntity, EnrollRelationId> {

    // Custom query to check if an enrollment relation exists for a specific user and course
    boolean existsByUserIdAndClassCodeAndQuarterAndYear(String userId, String classCode, String quarter, String year);

    // Custom query to find all enrollment relations for a specific user
    List<EnrollRelationEntity> findAllByUserId(String userId);

    // Custom query to find all enrollment relations for a specific course
    List<EnrollRelationEntity> findAllByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);
}
