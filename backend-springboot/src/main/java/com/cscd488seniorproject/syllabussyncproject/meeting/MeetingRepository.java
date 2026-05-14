package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    
    // Basic field queries
    List<Meeting> findByClassCode(String classCode);
    List<Meeting> findByRequesterId(String requesterId);
    List<Meeting> findByRecipientId(String recipientId);
    List<Meeting> findByStatus(String status);
    
    // Date range queries
    List<Meeting> findByMeetingDateBetween(LocalDate start, LocalDate end);
    List<Meeting> findByClassCodeAndMeetingDateBetween(String classCode, LocalDate start, LocalDate end);
    
    // Custom queries for user-centric searches
    @Query("SELECT m FROM Meeting m WHERE (m.requesterId = :userId OR m.recipientId = :userId) ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<Meeting> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT m FROM Meeting m WHERE (m.requesterId = :userId OR m.recipientId = :userId) AND m.meetingDate BETWEEN :startDate AND :endDate ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<Meeting> findByUserIdAndDateBetween(@Param("userId") String userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Recipient-specific queries
    List<Meeting> findByRecipientIdAndMeetingDate(String recipientId, LocalDate meetingDate);
    
    @Query("SELECT m FROM Meeting m WHERE m.recipientId = :recipientId AND m.status = :status ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<Meeting> findByRecipientIdAndStatus(@Param("recipientId") String recipientId, @Param("status") String status);
    
    // Sorted queries
    List<Meeting> findAllByOrderByMeetingDateAscStartTimeAsc();
}