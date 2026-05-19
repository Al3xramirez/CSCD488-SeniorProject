package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnrollRelationRepository extends JpaRepository<EnrollRelationEntity, EnrollRelationId> {

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM EnrollRelationEntity e
        WHERE e.id.userId = :userId
        AND e.id.classCode = :classCode
        AND e.id.quarter = :quarter
        AND e.id.year = :year
    """)
    boolean existsByUserIdAndClassCodeAndQuarterAndYear(
        @Param("userId") String userId,
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    @Query("SELECT e FROM EnrollRelationEntity e WHERE e.id.userId = :userId")
    List<EnrollRelationEntity> findByUserId(@Param("userId") String userId);

    @Query("""
        SELECT e FROM EnrollRelationEntity e
        WHERE e.id.classCode = :classCode
        AND e.id.quarter = :quarter
        AND e.id.year = :year
    """)
    List<EnrollRelationEntity> findByClassCodeAndQuarterAndYear(
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    @Query("""
        DELETE FROM EnrollRelationEntity e
        WHERE e.id.classCode = :classCode
        AND e.id.quarter = :quarter
        AND e.id.year = :year
    """)
    void deleteAllByClassCodeAndQuarterAndYear(
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    @Query("SELECT e FROM EnrollRelationEntity e WHERE e.id.classCode = :classCode")
    List<EnrollRelationEntity> findByClassCode(@Param("classCode") String classCode);

    @Query("""
        SELECT COUNT(e) FROM EnrollRelationEntity e
        WHERE e.id.classCode = :classCode
        AND e.id.quarter = :quarter
        AND e.id.year = :year
    """)
    long countByClassCodeAndQuarterAndYear(
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    @Query("""
        SELECT e FROM EnrollRelationEntity e
        WHERE e.id.classCode = :classCode
        AND e.id.quarter = :quarter
        AND e.id.year = :year
    """)
    List<EnrollRelationEntity> findAllByClassCodeAndQuarterAndYear(
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    //void deleteAllByClassCodeAndQuarterAndYear(String classCode, String quarter, Integer year);
}
