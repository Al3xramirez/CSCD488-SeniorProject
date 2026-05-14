package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByClassCode(String classCode);
    List<Meeting> findByRequesterId(String requesterId);
    List<Meeting> findByRecipientId(String recipientId);
    List<Meeting> findByStatus(String status);
    List<Meeting> findByMeetingDateBetween(LocalDate start, LocalDate end);
    List<Meeting> findByClassCodeAndMeetingDateBetween(String classCode, LocalDate start, LocalDate end);
}