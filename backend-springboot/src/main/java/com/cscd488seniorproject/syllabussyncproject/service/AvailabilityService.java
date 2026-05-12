package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.entity.AvailabilityStatus;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing user availability status.
 * This service allows users with the role of PROFESSOR or TA to update their availability status, 
 * which can be one of AVAILABLE, IDLE, or DND (Do Not Disturb). 
 * The availability status is stored in the UserAccountEntity and can be retrieved by authenticated users.
 */

@Service
public class AvailabilityService {

    private final UserAccountRepository userRepo;

    // Constructor injection of the UserAccountRepository
    public AvailabilityService(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    // Get the availability status of the authenticated user. If not set, defaults to AVAILABLE.
    @Transactional(readOnly = true)
    public AvailabilityStatus getMyAvailability(String emailRaw) {
        UserAccountEntity me = requireUserByEmail(emailRaw);
        AvailabilityStatus status = me.getAvailabilityStatus();
        return status == null ? AvailabilityStatus.AVAILABLE : status;
    }

    // Update the availability status of the authenticated user.
    @Transactional
    public AvailabilityStatus updateMyAvailability(String emailRaw, String statusRaw) {
        UserAccountEntity me = requireUserByEmail(emailRaw);
        requireRole(me, Set.of("PROFESSOR", "TA"));

        AvailabilityStatus status = parseAvailability(statusRaw);
        me.setAvailabilityStatus(status);
        userRepo.save(me);
        return status;
    }

    // Helper method to get the UserAccountEntity by email, throwing 401 if not authenticated or user not found.
    private UserAccountEntity requireUserByEmail(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    // Helper method to normalize role strings and check if the user's role is in the allowed set, throwing 403 if not.
    private static String normalizeRole(String role) {
        return (role == null ? "" : role.trim().toUpperCase(Locale.ROOT));
    }

    // Helper method to check if the user's role is in the allowed set, throwing 403 if not.
    private static void requireRole(UserAccountEntity user, Set<String> allowed) {
        String role = normalizeRole(user == null ? null : user.getRole());
        if (!allowed.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    // Helper method to parse availability status from a string, throwing 400 if invalid.
    private static AvailabilityStatus parseAvailability(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status required");
        }

        return switch (v) {
            case "AVAILABLE" -> AvailabilityStatus.AVAILABLE;
            case "IDLE" -> AvailabilityStatus.IDLE;
            case "DND" -> AvailabilityStatus.DND;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid availability status");
        };
    }
}
