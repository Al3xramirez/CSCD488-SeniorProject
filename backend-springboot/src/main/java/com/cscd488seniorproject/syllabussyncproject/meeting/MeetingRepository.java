package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {
    
    List<MeetingEntity> findByClassCode(String classCode);
    List<MeetingEntity> findByRequesterId(String requesterId);
    List<MeetingEntity> findByRecipientId(String recipientId);
    List<MeetingEntity> findByMeetingDateBetween(LocalDate start, LocalDate end);
    List<MeetingEntity> findByClassCodeAndMeetingDateBetween(String classCode, LocalDate start, LocalDate end);
    
    @Query("SELECT m FROM MeetingEntity m WHERE (m.requesterId = :userId OR m.recipientId = :userId) ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<MeetingEntity> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT m FROM MeetingEntity m WHERE (m.requesterId = :userId OR m.recipientId = :userId) AND m.meetingDate BETWEEN :startDate AND :endDate ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<MeetingEntity> findByUserIdAndDateBetween(@Param("userId") String userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    List<MeetingEntity> findByRecipientIdAndMeetingDate(String recipientId, LocalDate meetingDate);
    List<MeetingEntity> findAllByOrderByMeetingDateAscStartTimeAsc();
    List<MeetingEntity> findByRequesterIdOrRecipientIdAndMeetingDate(String userEmail, String userEmail2,
            LocalDate localDate);
}