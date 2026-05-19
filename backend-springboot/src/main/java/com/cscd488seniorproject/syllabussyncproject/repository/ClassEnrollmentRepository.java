package com.cscd488seniorproject.syllabussyncproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentId;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentEntity, ClassEnrollmentId> {
    
    @Query("SELECT e FROM ClassEnrollmentEntity e WHERE e.id.userId = :userId")
    List<ClassEnrollmentEntity> findByUserId(@Param("userId") String userId);

    @Query("SELECT e FROM ClassEnrollmentEntity e WHERE e.id.classCode = :classCode")
    List<ClassEnrollmentEntity> findByClassCode(@Param("classCode") String classCode);

    @Query("SELECT e FROM ClassEnrollmentEntity e WHERE e.id.userId = :userId AND e.id.classCode = :classCode")
    Optional<ClassEnrollmentEntity> findByUserIdAndClassCode(
        @Param("userId") String userId,
        @Param("classCode") String classCode
    );

    @Query("""
        SELECT e FROM ClassEnrollmentEntity e 
        WHERE e.id.classCode = :classCode 
        AND e.id.quarter = :quarter 
        AND e.id.year = :year
    """)
    List<ClassEnrollmentEntity> findByClassCodeAndQuarterAndYear(
        @Param("classCode") String classCode,
        @Param("quarter") String quarter,
        @Param("year") Integer year
    );

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM ClassEnrollmentEntity e
        WHERE e.id.userId = :userId 
        AND e.id.classCode = :classCode
    """)
    boolean existsByUserIdAndClassCode(
        @Param("userId") String userId,
        @Param("classCode") String classCode
    );

    @Transactional
    @Query("""
        DELETE FROM ClassEnrollmentEntity e
        WHERE e.id.userId = :userId 
        AND e.id.classCode = :classCode
    """)
    void deleteByUserIdAndClassCode(
        @Param("userId") String userId,
        @Param("classCode") String classCode
    );

    @Query("""
        SELECT COUNT(e) FROM ClassEnrollmentEntity e
        WHERE e.id.classCode = :classCode
    """)
    long countByClassCode(@Param("classCode") String classCode);

    @Query("SELECT e FROM ClassEnrollmentEntity e WHERE e.id.classCode = :classId")
    List<ClassEnrollmentEntity> findByClassId(@Param("classId") String classId);
}
