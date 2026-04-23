package com.cscd488seniorproject.syllabussyncproject.dto;

// DTO for summarizing student information in a class
public class StudentSummaryDTO {
    public String userId;
    public String email;
    public String firstName;
    public String lastName;
    public String role; // added this to distinguish between students and TAs in the roster

    public StudentSummaryDTO() {}

    public StudentSummaryDTO(String userId, String email, String firstName, String lastName, String role) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
}
