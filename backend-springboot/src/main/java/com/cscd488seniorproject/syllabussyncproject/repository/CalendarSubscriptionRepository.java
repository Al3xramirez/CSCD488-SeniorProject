package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.CalendarSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscription, Long> {

    Optional<CalendarSubscription> findByUserIdAndProvider(String userId, String provider);

    List<CalendarSubscription> findByUserIdAndIsEnabledTrue(String userId);

    List<CalendarSubscription> findByIsEnabledTrue();
}

