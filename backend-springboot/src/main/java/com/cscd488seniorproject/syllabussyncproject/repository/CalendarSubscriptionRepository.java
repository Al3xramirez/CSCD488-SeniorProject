package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscriptionEntity, Long> {

    Optional<CalendarSubscriptionEntity> findByUserIdAndProvider(String userId, String provider);

    List<CalendarSubscriptionEntity> findByUserIdAndIsEnabledTrue(String userId);

    List<CalendarSubscriptionEntity> findByIsEnabledTrue();
}

