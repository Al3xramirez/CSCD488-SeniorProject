package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.Syllabus;
import com.cscd488seniorproject.syllabussync.entity.SyllabusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, SyllabusId> {
}