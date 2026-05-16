package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "officehour")
public class OfficeHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long officeHourID;

    @ManyToOne
    @JoinColumn(name = "UserID")
    private UserAccount user;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "ClassCode", referencedColumnName = "classCode"),
        @JoinColumn(name = "Quarter", referencedColumnName = "quarter"),
        @JoinColumn(name = "Year", referencedColumnName = "year")
    })
    private Course course;

    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}