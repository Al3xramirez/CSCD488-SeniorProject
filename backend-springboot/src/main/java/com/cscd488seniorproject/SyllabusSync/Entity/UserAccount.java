package com.cscd488seniorproject.syllabussync.entity;

@Entity
@Table(name = "useraccount")
public class UserAccount {

    @Id
    private String userID;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fname;
    private String lname;
    private String department;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        PROFESSOR,
        TA,
        STUDENT
    }
}