package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.ExternalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExternalEventRepository extends JpaRepository<ExternalEvent, Long> {

    Optional<ExternalEvent> findBySubscription_SubscriptionIdAndIcalUidAndRecurrenceId(
        Long subscriptionId,
        String icalUid,
        String recurrenceId
    );

    List<ExternalEvent> findBySubscription_SubscriptionIdAndIsCancelledFalse(Long subscriptionId);

    @Query("""
        select e
        from ExternalEvent e
        where e.subscription.userId = :userId
          and e.subscription.isEnabled = true
          and e.startAt between :from and :to
        order by e.startAt asc
        """)
    List<ExternalEvent> findMergedEnabledEventsForUserInWindow(
        @Param("userId") String userId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}

