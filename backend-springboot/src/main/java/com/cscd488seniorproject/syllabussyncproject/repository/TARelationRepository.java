package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.TARelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TARelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TARelationRepository extends JpaRepository<TARelationEntity, TARelationId> {

    boolean existsByUserIdAndClassCodeAndQuarterAndYear(String userId, String classCode, String quarter, String year);

    List<TARelationEntity> findAllByUserId(String userId);

    List<TARelationEntity> findAllByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);

    long deleteAllByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);
}
