package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.entity.Meeting;
import com.cscd488seniorproject.syllabussyncproject.repository.MeetingRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

private final MeetingRepository repository;

public MeetingController(MeetingRepository repository) {
    this.repository = repository;
}


@GetMapping("/start={start}&end={end}")
public List<Meeting> getMeetings(
        @RequestParam LocalDate start,
        @RequestParam LocalDate end) {

        LocalDate startDate = LocalDate.parse(String.valueOf(start).substring(0, 10));
        LocalDate endDate = LocalDate.parse(String.valueOf(end).substring(0, 10));
    return repository.findByMeetingDateBetween(startDate, endDate);
}

@PostMapping
public Meeting createMeeting(@RequestBody Meeting meeting) {

    return repository.save(meeting);

}

}
