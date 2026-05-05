package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.OfficeHoursExceptionEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeHoursExceptionRepository extends JpaRepository<OfficeHoursExceptionEntity, Long> {

    List<OfficeHoursExceptionEntity> findAllByUserId(String userId);

    List<OfficeHoursExceptionEntity> findAllByUserIdAndExceptionDateBetween(String userId, LocalDate from, LocalDate to);

    boolean existsByIdAndUserId(Long id, String userId);
}
