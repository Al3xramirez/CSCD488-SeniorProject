package com.cscd488seniorproject.syllabussyncproject.dto;

import java.time.Instant;

public class CalendarEventResponse {
    public Long eventId;
    public String title;
    public Instant startAt;
    public Instant endAt;
    public String location;
    public String source;
}
