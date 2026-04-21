package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyllabusRepository extends JpaRepository<SyllabusEntity, Long> {

    Optional<SyllabusEntity> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
