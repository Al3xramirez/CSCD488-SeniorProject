package com.cscd488seniorproject.syllabussyncproject.dto;

public class SyllabusSaveRequest {
    /** The course join code used as the course identifier. */
    public String courseId;

    /** { days: string[], startTime: string, endTime: string, location: string } */
    public Object classMeetingTimes;

    /** [{ days: string[], startTime: string, endTime: string, location: string }] */
    public Object officeHours;

    /** [{ letter: string, range: string }] */
    public Object gradeScale;

    /** [{ component: string, weight: string }] */
    public Object gradeBreakdown;

    /** string[] — mandatory pass requirements beyond the grade scale */
    public Object passConditions;

    /** { tracked: string, affectsGrade: string, details: string } */
    public Object attendance;

    /** [{ name: string, date: string }] */
    public Object dueDates;

    public String lateWorkPolicy;

    /** null if not mentioned in syllabus */
    public String aiPolicy;
}
