package com.cscd488seniorproject.syllabussyncproject.controllers;

//This class  is used as a security measure to prevent users from sending JSON objects with extra fields that we dont want
//Not sure if this will be needed in the future though
public class SignupRequest {

    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String role; // "student" / "ta" / "professor" from Vue
}
