package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.TeachesRelation;
import com.cscd488seniorproject.syllabussync.entity.TeachesRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachesRelationRepository extends JpaRepository<TeachesRelation, TeachesRelationId> {

    List<TeachesRelation> findByUserID(String userID);

    List<TeachesRelation> findByClassCodeAndQuarterAndYear(
        String classCode, String quarter, String year);
}