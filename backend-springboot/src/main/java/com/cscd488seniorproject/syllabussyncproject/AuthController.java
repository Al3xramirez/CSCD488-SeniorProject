package com.cscd488seniorproject.syllabussyncproject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Map<String, Account> accountsByEmail = new ConcurrentHashMap<>();

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@RequestBody AuthRequest request) {
        if (isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Username, email, and password are required."));
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (!normalizedEmail.endsWith("@ewu.edu")) {
            return ResponseEntity.badRequest().body(new ApiResponse("Please use an EWU email address."));
        }

        Account existing = accountsByEmail.putIfAbsent(
                normalizedEmail,
                new Account(request.username().trim(), normalizedEmail, hashPassword(request.password())));

        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse("Account already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Signup successful."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody AuthRequest request) {
        if (isBlank(request.email()) || isBlank(request.password())) {
            return ResponseEntity.badRequest().body(new ApiResponse("Email and password are required."));
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        Account account = accountsByEmail.get(normalizedEmail);
        if (account == null || !account.passwordHash().equals(hashPassword(request.password()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse("Invalid email or password."));
        }

        return ResponseEntity.ok(new ApiResponse("Login successful. Welcome, " + account.username() + "."));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    record AuthRequest(String username, String email, String password) {
    }

    record Account(String username, String email, String passwordHash) {
    }

    record ApiResponse(String message) {
    }
}
