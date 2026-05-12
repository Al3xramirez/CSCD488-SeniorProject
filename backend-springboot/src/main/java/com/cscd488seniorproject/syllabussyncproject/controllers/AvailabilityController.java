package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.dto.UpdateAvailabilityRequestDTO;
import com.cscd488seniorproject.syllabussyncproject.entity.AvailabilityStatus;
import com.cscd488seniorproject.syllabussyncproject.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing user availability status.
 * This controller allows authenticated users to get and update their availability status,
 * which can be one of AVAILABLE, IDLE, or DND
 */

@RestController
@RequestMapping("/api/me")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // Constructor injection of the AvailabilityService
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // Get the availability status of the authenticated user. If not set, defaults to AVAILABLE.
    @GetMapping("/availability")
    public ResponseEntity<?> get(Authentication auth) {
        String email = auth == null ? null : auth.getName();
        AvailabilityStatus status = availabilityService.getMyAvailability(email);
        return ResponseEntity.ok(new AvailabilityResponse(status.name()));
    }

    // Update the availability status of the authenticated user.
    @PutMapping("/availability")
    public ResponseEntity<?> update(Authentication auth, @RequestBody UpdateAvailabilityRequestDTO req) {
        String email = auth == null ? null : auth.getName();
        AvailabilityStatus status = availabilityService.updateMyAvailability(email, req == null ? null : req.status);
        return ResponseEntity.ok(new AvailabilityResponse(status.name()));
    }

    // Good ol record for the availability response
    public record AvailabilityResponse(String availabilityStatus) {}
}
