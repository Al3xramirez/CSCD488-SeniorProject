package com.cscd488seniorproject.syllabussync;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ServerController {

    

    public String handleSignupForm(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password) {
        // Your SQL logic goes here
        System.out.println("Received: " + email);
        System.out.println("Received: " + password);
        System.out.println("Received: " + username);
        
        return "/login";
    }

    public String handleLoginForm(
            @RequestParam String email, 
            @RequestParam String password) {
        // Your SQL logic goes here
        //if okay return home with login auth
        //if not return login with some kind of failure notice
        System.out.println("Received: " + email);
        System.out.println("Received: " + password);
        return "/Dashboard"; // Tells Spring to show success-page.html
    }
}