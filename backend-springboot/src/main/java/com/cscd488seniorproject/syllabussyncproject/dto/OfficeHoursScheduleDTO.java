package com.cscd488seniorproject.syllabussyncproject.dto;

public class OfficeHoursScheduleDTO {
    public Long id;
    public String dayOfWeek;   // MON, TUE, WED, THU, FRI, SAT, SUN
    public String startTime;   // HH:mm
    public String endTime;     // HH:mm
    public String quarter;
    public int year;

    public OfficeHoursScheduleDTO() {}

    public OfficeHoursScheduleDTO(Long id, String dayOfWeek, String startTime, String endTime, String quarter, int year) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.quarter = quarter;
        this.year = year;
    }
}
