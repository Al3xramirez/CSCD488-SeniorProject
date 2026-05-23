package com.cscd488seniorproject.syllabussyncproject.entity;
import java.time.LocalDateTime;

import jakarta.persistence.*;

// This class represents the UserAccount table in the database
@Entity
@Table(name = "useraccount")
public class UserAccountEntity {

    @Id
    @Column(name = "UserID", length = 20, nullable = false, unique = true)
    private String userId;

    @Column(name = "Email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", columnDefinition = "text", nullable = false)
    private String passwordHash;

    @Column(name = "Fname", length = 50)
    private String firstName;

    @Column(name = "Lname", length = 50)
    private String lastName;

    @Column(name = "Role", length = 20, nullable = false)
    private String role; // STUDENT, TA, or PROFESSOR

    @Column(name = "AvailabilityStatus", length = 20, nullable = false)
    private String availabilityStatus; // AVAILABLE, IDLE, DND, HIDDEN

    @Column(name = "Department", length = 50)
    private String department;

    @Lob
    @Column(name = "ProfilePhoto", columnDefinition = "LONGBLOB")
    private byte[] profilePhoto;

    @Column(name = "ProfilePhotoContentType", length = 100)
    private String profilePhotoContentType;

    @Column(name = "ProfilePhotoUpdatedAt")
    private LocalDateTime profilePhotoUpdatedAt;

    // Getters and Setters
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public byte[] getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(byte[] profilePhoto) { this.profilePhoto = profilePhoto; }

    public String getProfilePhotoContentType() { return profilePhotoContentType; }
    public void setProfilePhotoContentType(String profilePhotoContentType) { this.profilePhotoContentType = profilePhotoContentType; }

    public LocalDateTime getProfilePhotoUpdatedAt() { return profilePhotoUpdatedAt; }
    public void setProfilePhotoUpdatedAt(LocalDateTime profilePhotoUpdatedAt) { this.profilePhotoUpdatedAt = profilePhotoUpdatedAt; }
}
