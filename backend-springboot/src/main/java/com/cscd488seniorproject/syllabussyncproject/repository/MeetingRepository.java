package com.cscd488seniorproject.syllabussync.repository;

import com.cscd488seniorproject.syllabussync.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByCourse_ClassCodeAndCourse_QuarterAndCourse_Year(
        String classCode, String quarter, String year);

    List<Meeting> findByRequester_UserID(String requesterID);

    List<Meeting> findByRecipient_UserID(String recipientID);

    List<Meeting> findByMeetingDate(LocalDate date);

    List<Meeting> findByStatus(Meeting.Status status);
}