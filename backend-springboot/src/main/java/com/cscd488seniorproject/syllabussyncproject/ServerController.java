package com.cscd488SeniorProject.SyllabusSync;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ServerController {
    public static void main(String args[]){
        //establish connection with server
    }
    
    @PostMapping("/signup")
    public String handleForm(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password) {
        // Your SQL logic goes here
        System.out.println("Received: " + email);
        System.out.println("Received: " + password);
        System.out.println("Received: " + firstName);
        System.out.println("Received: " + lastName);
        
        return "/login"; // Tells Spring to show success-page.html
    }
    @PostMapping("/login")
    public String handleForm(
            @RequestParam String email, 
            @RequestParam String password) {
        // Your SQL logic goes here
        //if okay return home with login auth
        //if not return login with some kind of failure notice
        System.out.println("Received: " + email);
        System.out.println("Received: " + password);
        return "/home"; // Tells Spring to show success-page.html
    }
}