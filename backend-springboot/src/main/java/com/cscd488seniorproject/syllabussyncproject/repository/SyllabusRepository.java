package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyllabusRepository extends JpaRepository<SyllabusEntity, SyllabusId> {

    Optional<SyllabusEntity> findByClassCodeAndQuarterAndYear(String classCode, String quarter, String year);
}
