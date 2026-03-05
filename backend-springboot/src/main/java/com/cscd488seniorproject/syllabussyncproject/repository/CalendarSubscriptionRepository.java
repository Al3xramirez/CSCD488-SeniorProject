package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.CalendarSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscription, Long> {

    List<CalendarSubscription> findByUserID(String userID);

    List<CalendarSubscription> findByProvider(String provider);
}