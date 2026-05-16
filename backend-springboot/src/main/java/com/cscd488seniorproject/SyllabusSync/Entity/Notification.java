package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationID;

    @ManyToOne
    @JoinColumn(name = "UserID")
    private UserAccount user;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Instant createdAt;

    private Boolean isRead;
}