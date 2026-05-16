package com.cscd488seniorproject.syllabussync.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingsRepository extends JpaRepository<Meetings, Integer> {

    List<Meetings> findByRequesterID(Integer requesterID);

    List<Meetings> findByRecipientID(Integer recipientID);

    List<Meetings> findByStatus(String status);
}