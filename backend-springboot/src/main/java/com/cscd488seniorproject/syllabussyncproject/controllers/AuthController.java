package com.cscd488seniorproject.syllabussyncproject.controllers;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;


// This class is used to handle authentication requests from the frontend, such as signup and check-auth
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserAccountRepository userAccountRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserAccountRepository userAccountRepo, PasswordEncoder passwordEncoder) {
        this.userAccountRepo = userAccountRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // This endpoint is used by the frontend to create new user accounts

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        
       // email is required and must be unique
        String email = req.email == null ? "" : req.email.trim().toLowerCase(Locale.ROOT);

        if (email.isBlank() || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body("Email and password required");
        }
        // Check if email is already in use
        if (userAccountRepo.existsByEmail(email)) {
            return ResponseEntity.status(409).body("Email already in use");
        }
        // Default role is STUDENT if not provided, otherwise use provided role (after trimming and converting to uppercase)
        String role = (req.role == null ? "STUDENT" : req.role.trim().toUpperCase(Locale.ROOT));

        // DB constraint expects EXACT: PROFESSOR / TA / STUDENT
        if (!role.equals("STUDENT") && !role.equals("TA") && !role.equals("PROFESSOR")) {
            return ResponseEntity.badRequest().body("Invalid role");
        }

        // finally creates the user account
        UserAccountEntity user = new UserAccountEntity();
        user.setUserId(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setFirstName(req.firstName);
        user.setLastName(req.lastName);
        user.setRole(role);

        // Save the new user to the database
        userAccountRepo.save(user);

        // Return success response to the frontend
        return ResponseEntity.ok("Account created");
    }

    // This endpoint is used by the frontend to check if the user is currently authenticated (session cookie is valid)

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth(Authentication auth) {
        
        // If the user is authenticated, return 200 OK with a message. Otherwise return 401 Unauthorized.
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return ResponseEntity.ok("Authenticated");
        }
        return ResponseEntity.status(401).body("Not authenticated");
    }

    // Returns the currently authenticated user's basic info (used by the frontend for role-based UI)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {

        // If the user is not authenticated, return 401 Unauthorized
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        // Gets the user's email from the authentication object, then looks up the user in the database to get their role and name.
        String email = auth.getName() == null ? "" : auth.getName().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        // First try to get the role from the database, since that's the source of truth. If it fails for some reason, fallback to getting the role from the authentication.
        UserAccountEntity user = userAccountRepo.findByEmail(email).orElse(null);
        String role = null;
        if (user != null && user.getRole() != null && !user.getRole().isBlank()) {
            role = user.getRole().trim().toUpperCase(Locale.ROOT);
        } else {
            // Fallback to authority if DB lookup fails for some reason
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga != null && ga.getAuthority() != null && ga.getAuthority().startsWith("ROLE_")) {
                    role = ga.getAuthority().substring("ROLE_".length());
                    break;
                }
            }
        }
        // Return the user's email, role, first name, and last name to the frontend. The frontend can use this info for role-based UI.
        return ResponseEntity.ok(new MeResponse(
            email,
            role,
            user == null ? null : user.getFirstName(),
            user == null ? null : user.getLastName()
        ));
    }
    // This record class is used to represent the response from the /me endpoint, which includes the user's email, role, first name, and last name. 
    // Inline suggestions suggested making this a record.
    public record MeResponse(String email, String role, String firstName, String lastName) {}

}
