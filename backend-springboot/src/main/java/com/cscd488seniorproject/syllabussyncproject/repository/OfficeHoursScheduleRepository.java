package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.OfficeHoursScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeHoursScheduleRepository extends JpaRepository<OfficeHoursScheduleEntity, Long> {

    List<OfficeHoursScheduleEntity> findAllByUserId(String userId);

    List<OfficeHoursScheduleEntity> findAllByUserIdAndQuarterAndYear(String userId, String quarter, String year);

    boolean existsByIdAndUserId(Long id, String userId);
}
