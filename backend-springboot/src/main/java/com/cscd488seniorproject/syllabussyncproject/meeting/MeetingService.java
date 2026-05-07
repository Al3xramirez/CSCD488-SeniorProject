package com.cscd488seniorproject.syllabussyncproject.meeting;

import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public List<Meeting> generateMeetings(MeetingRequest req) {
        List<Meeting> meetings = new ArrayList<>();

        LocalDate current = req.getStartDate();
        while (!current.isAfter(req.getEndDate())) {
            DayOfWeek day = current.getDayOfWeek();
            if (req.getDaysOfWeek().contains(day)) {
                Meeting m = new Meeting();
                m.setDate(current);
                m.setStartTime(req.getStartTime());
                m.setEndTime(req.getEndTime());
                m.setClassId(req.getClassId());
                meetings.add(m);
            }
            current = current.plusDays(1);
        }

        return meetings;
    }

    public void createRecurringMeetings(MeetingRequest req) {
        if (!req.isValid()) {
            throw new IllegalArgumentException("Invalid meeting request");
        }
        List<Meeting> meetings = generateMeetings(req);
        meetingRepository.saveAll(meetings);
    }
}