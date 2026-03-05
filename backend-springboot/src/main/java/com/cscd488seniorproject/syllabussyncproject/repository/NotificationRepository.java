package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.ExternalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalEventRepository extends JpaRepository<ExternalEvent, Long> {

    List<ExternalEvent> findBySubscriptionID(Long subscriptionID);

    List<ExternalEvent> findByIsCancelled(boolean isCancelled);
}