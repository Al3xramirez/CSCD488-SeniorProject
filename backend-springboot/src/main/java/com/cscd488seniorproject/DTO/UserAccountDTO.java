package com.cscd488seniorproject.syllabussync.dto;

public class UserAccountDTO {
    private String userID;
    private String email;
    private String passwordHash;
    private String fname;
    private String lname;
    private String department;
    private String role;

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }
    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}