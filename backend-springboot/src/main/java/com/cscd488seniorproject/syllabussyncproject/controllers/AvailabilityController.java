package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* This controller is used to handle requests related to user availability status,
which is a feature that allows TAs and Professors to set their current availability (AVAILABLE, IDLE, DND, HIDDEN) that can be seen by students in the class roster. 
This is meant to help students know when their TAs or Professors are available for questions or help.
Only TAs and Professors can change their availability status, 
and it can be changed at any time. 
The frontend will use this information to display different indicators next to the names of TAs and Professors in the roster, 
and may also use it to sort or filter the roster based on availability.
*/

@RestController
@RequestMapping("/api/me")
public class AvailabilityController {

    private static final Set<String> VALID_STATUSES = Set.of("AVAILABLE", "IDLE", "DND", "HIDDEN");

    private final UserAccountRepository userRepo;

    public AvailabilityController(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    // Endpoint for TAs and Professors to set their availability status
    // [1] checks if the user is authenticated and has the role of TA or PROFESSOR, 
    // [2] validates the provided status, 
    // [3] updates the user's availability status in the database.
    @PostMapping("/availability")
    public ResponseEntity<?> setMyAvailability(Authentication auth, @RequestBody AvailabilityRequest req) {
        UserAccountEntity me = requireUser(auth);

        String role = normalize(me.getRole());
        if (!role.equals("TA") && !role.equals("PROFESSOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only TAs and Professors can change availability");
        }

        String status = normalize(req == null ? null : req.status);
        if (!VALID_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body("Invalid status (use AVAILABLE, IDLE, DND, HIDDEN)");
        }

        me.setAvailabilityStatus(status);
        userRepo.save(me);

        return ResponseEntity.ok(new AvailabilityResponse(status));
    }

    // Records used for the request and response bodies of the setMyAvailability endpoint

    public record AvailabilityRequest(String status) {}

    public record AvailabilityResponse(String availabilityStatus) {}


    // Helper method to get the currently authenticated user from the Authentication object

    private UserAccountEntity requireUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = auth.getName() == null ? "" : auth.getName().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    // Helper method to normalize strings by trimming and converting to uppercase, 
    // treating null as empty string
    private static String normalize(String v) {
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }
}
