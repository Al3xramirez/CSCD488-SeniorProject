package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.AssistsRelation;
import com.cscd488seniorproject.syllabussync.entity.AssistsRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssistsRelationRepository extends JpaRepository<AssistsRelation, AssistsRelationId> {

    List<AssistsRelation> findByUserID(String userID);

    List<AssistsRelation> findByClassCodeAndQuarterAndYear(
        String classCode, String quarter, String year);
}