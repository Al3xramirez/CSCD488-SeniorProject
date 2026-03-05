package com.cscd488seniorproject.syllabussync.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "meeting")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long meetingID;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "ClassCode", referencedColumnName = "classCode"),
        @JoinColumn(name = "Quarter", referencedColumnName = "quarter"),
        @JoinColumn(name = "Year", referencedColumnName = "year")
    })
    private Course course;

    @ManyToOne
    @JoinColumn(name = "RequesterID")
    private UserAccount requester;

    @ManyToOne
    @JoinColumn(name = "RecipientID")
    private UserAccount recipient;

    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}