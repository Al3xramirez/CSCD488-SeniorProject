package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.dto.SyllabusSaveRequest;
import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.service.ClaudeService;
import com.cscd488seniorproject.syllabussyncproject.service.SyllabusService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}/syllabus")
@RequiredArgsConstructor
public class SyllabusController {

    private final ClaudeService claudeService;
    private final SyllabusService syllabusService;
    private final UserAccountRepository userAccountRepository;

    // Feature flag to prevent spending AI credits in production.
    // Set `SYLLABUS_IMPORT_ENABLED=false` in Azure to disable parsing.
    @Value("${SYLLABUS_IMPORT_ENABLED:true}")
    private boolean syllabusImportEnabled;

    /**
     * POST /api/courses/{courseId}/syllabus/parse
     * Accepts the raw PDF as multipart/form-data, base64-encodes it, sends it to
     * Claude, and returns the parsed JSON with per-field confidence scores.
     * Professor-only.
     */
    @PostMapping("/parse")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> parse(
            @RequestParam("file") MultipartFile file) {
        try {
            if (!syllabusImportEnabled) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Syllabus import is temporarily disabled");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("PDF file is required");
            }

            byte[] bytes = file.getBytes();
            String pdfBase64 = Base64.getEncoder().encodeToString(bytes);

            JsonNode parsed = claudeService.parseSyllabus(pdfBase64);
            return ResponseEntity.ok(parsed);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    /**
     * POST /api/courses/{courseId}/syllabus
     * Persists the confirmed syllabus fields for the course.
     * Professor-only.
     */
    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<?> save(
            @PathVariable String courseId,
            @RequestBody SyllabusSaveRequest req,
            Authentication auth) {
        try {
            req.courseId = courseId;
            String professorId = resolveUserId(auth);
            SyllabusEntity saved = syllabusService.saveOrUpdate(req, professorId);
            return ResponseEntity.ok(Map.of(
                    "syllabusId", saved.getSyllabusId(),
                    "courseId", saved.getJoinCode(),
                    "updatedAt", saved.getUpdatedAt()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    /**
     * GET /api/courses/{courseId}/syllabus
     * Returns the saved syllabus for the course.
     * Any authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> get(
            @PathVariable String courseId,
            Authentication auth) {
        try {
            Map<String, Object> syllabus = syllabusService.getByCourseId(courseId);
            return ResponseEntity.ok(syllabus);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    private String resolveUserId(Authentication auth) {
        String email = auth.getName();
        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return user.getUserId();
    }
}
