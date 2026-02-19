package com.cscd488SeniorProject.SyllabusSync;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ServerController {
    // This method handles the form submission because the URL matches the 'action'
    public static void main(String args[]){
        //establish connection with server
    }
    
    @PostMapping("/register")
    public String handleForm(@RequestParam String username) {
        // Your SQL logic goes here
        System.out.println("Received: " + username);
        return "success-page"; // Tells Spring to show success-page.html
    }
}