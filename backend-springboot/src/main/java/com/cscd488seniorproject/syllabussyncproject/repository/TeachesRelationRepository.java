package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeachesRelationRepository extends JpaRepository<TeachesRelationEntity, TeachesRelationId> {

    boolean existsByUserIdAndClassCodeAndQuarterAndYear(String userId, String classCode, String quarter, Integer year);

    List<TeachesRelationEntity> findAllByUserId(String userId);

    List<TeachesRelationEntity> findAllByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);

    long deleteAllByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);

    @Query("SELECT t FROM TeachesRelationEntity t WHERE t.classCode = :classCode")
    List<TeachesRelationEntity> findByClassCode(@Param("classCode") String classCode);
}
