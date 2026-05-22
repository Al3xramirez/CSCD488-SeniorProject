package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.AssistsRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.AssistsRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistsRelationRepository extends JpaRepository<AssistsRelationEntity, AssistsRelationId> {
    List<AssistsRelationEntity> findByUserId(String userId);
    List<AssistsRelationEntity> findByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);
    boolean existsByUserIdAndClassCodeAndQuarterAndYear(String userId, String classCode, String quarter, Integer year);
}