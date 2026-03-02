package com.cscd488seniorproject.syllabussyncproject.entity;
import jakarta.persistence.*;
import java.time.Instant;

// This class represents the UserAccount table in the database
@Entity
@Table(name = "useraccount")
public class UserAccountEntity {

    @Id
    @Column(name = "UserID", length = 20, nullable = false, unique = true)
    private String userId;

    @Column(name = "Email", length = 50, nullable = false, unique = true)
    private String email;

    @Column(name = "PasswordHash", columnDefinition = "text", nullable = false)
    private String passwordHash;

    @Column(name = "Fname", length = 50)
    private String firstName;

    @Column(name = "Lname", length = 50)
    private String lastName;

    @Column(name = "Role", length = 20, nullable = false)
    private String role; // STUDENT, TA, or PROFESSOR

    @Column(name = "CanvasIcalUrlEncrypted", columnDefinition = "text")
    private String canvasIcalUrlEncrypted;

    @Column(name = "CanvasLastSyncedAt")
    private Instant canvasLastSyncedAt;

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

    public String getCanvasIcalUrlEncrypted() { return canvasIcalUrlEncrypted; }
    public void setCanvasIcalUrlEncrypted(String canvasIcalUrlEncrypted) { this.canvasIcalUrlEncrypted = canvasIcalUrlEncrypted; }

    public Instant getCanvasLastSyncedAt() { return canvasLastSyncedAt; }
    public void setCanvasLastSyncedAt(Instant canvasLastSyncedAt) { this.canvasLastSyncedAt = canvasLastSyncedAt; }
}
