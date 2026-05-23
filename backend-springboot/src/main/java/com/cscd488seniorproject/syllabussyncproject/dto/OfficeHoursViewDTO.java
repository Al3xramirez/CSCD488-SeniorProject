package com.cscd488seniorproject.syllabussyncproject.dto;

import java.util.List;

// Student-facing view: who owns these hours + their schedule + exceptions
public class OfficeHoursViewDTO {
    public String userId;
    public String firstName;
    public String lastName;
    public String role;
    public List<OfficeHoursScheduleDTO> schedule;
    public List<OfficeHoursExceptionDTO> exceptions;

    public OfficeHoursViewDTO() {}

    public OfficeHoursViewDTO(String userId, String firstName, String lastName, String role,
                               List<OfficeHoursScheduleDTO> schedule, List<OfficeHoursExceptionDTO> exceptions) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.schedule = schedule;
        this.exceptions = exceptions;
    }
}
