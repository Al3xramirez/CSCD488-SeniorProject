package com.cscd488seniorproject.syllabussyncproject.dto;

public class OfficeHoursExceptionDTO {
    public Long id;
    public String exceptionDate;  // YYYY-MM-DD
    public String startTime;      // HH:mm, null = whole day
    public String endTime;        // HH:mm, null = whole day
    public boolean unavailable;
    public String note;

    public OfficeHoursExceptionDTO() {}

    public OfficeHoursExceptionDTO(Long id, String exceptionDate, String startTime, String endTime, boolean unavailable, String note) {
        this.id = id;
        this.exceptionDate = exceptionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.unavailable = unavailable;
        this.note = note;
    }
}
