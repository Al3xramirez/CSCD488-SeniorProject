package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.EnrollsRelation;
import com.cscd488seniorproject.syllabussync.entity.EnrollsRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollsRelationRepository extends JpaRepository<EnrollsRelation, EnrollsRelationId> {

    List<EnrollsRelation> findByUserID(String userID);

    List<EnrollsRelation> findByClassCodeAndQuarterAndYear(
        String classCode, String quarter, String year);
}