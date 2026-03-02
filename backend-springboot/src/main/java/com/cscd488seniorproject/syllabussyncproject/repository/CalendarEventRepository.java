package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {

    Optional<CalendarEventEntity> findByUserIdAndExternalUid(String userId, String externalUid);

    List<CalendarEventEntity> findByUserIdAndStartAtBetweenOrderByStartAtAsc(String userId, Instant from, Instant to);
}
