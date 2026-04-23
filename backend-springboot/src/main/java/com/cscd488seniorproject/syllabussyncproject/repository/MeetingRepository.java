package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByMeetingDateBetween(LocalDate startDate, LocalDate endDate);
    List<Meeting> findAllByOrderByMeetingDateAscStartTimeAsc();
    
    // Find meetings where user is requester or recipient
    @Query("SELECT m FROM Meeting m WHERE (m.requesterID = :userID OR m.recipientID = :userID) ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<Meeting> findByUserID(@Param("userID") String userID);
    
    @Query("SELECT m FROM Meeting m WHERE (m.requesterID = :userID OR m.recipientID = :userID) AND m.meetingDate BETWEEN :startDate AND :endDate ORDER BY m.meetingDate ASC, m.startTime ASC")
    List<Meeting> findByUserIDAndDateBetween(@Param("userID") String userID, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}