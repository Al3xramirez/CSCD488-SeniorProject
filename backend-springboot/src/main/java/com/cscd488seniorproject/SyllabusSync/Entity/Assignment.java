package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "assignment")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentID;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "ClassCode", referencedColumnName = "classCode"),
        @JoinColumn(name = "Quarter", referencedColumnName = "quarter"),
        @JoinColumn(name = "Year", referencedColumnName = "year")
    })
    private Course course;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant dueDate;

    private BigDecimal points;
    private BigDecimal weight;
    private BigDecimal estimatedEffortHours;

    private String category;
}