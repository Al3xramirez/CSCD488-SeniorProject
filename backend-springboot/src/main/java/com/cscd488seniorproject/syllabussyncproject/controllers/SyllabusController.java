package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.EnrollRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.SyllabusRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TeachesRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import com.cscd488seniorproject.syllabussyncproject.service.SyllabusImportService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusImportService syllabusImportService;
    private final UserAccountRepository userAccountRepository;
    private final SyllabusRepository syllabusRepository;
    private final TeachesRelationRepository teachesRelationRepository;
    private final EnrollRelationRepository enrollRelationRepository;

    public SyllabusController(
            SyllabusImportService syllabusImportService,
            UserAccountRepository userAccountRepository,
            SyllabusRepository syllabusRepository,
            TeachesRelationRepository teachesRelationRepository,
            EnrollRelationRepository enrollRelationRepository
    ) {
        this.syllabusImportService = syllabusImportService;
        this.userAccountRepository = userAccountRepository;
        this.syllabusRepository = syllabusRepository;
        this.teachesRelationRepository = teachesRelationRepository;
        this.enrollRelationRepository = enrollRelationRepository;
    }

    /*
        * Endpoint to import a syllabus PDF and extract its information.
        * Only authenticated users with the "PROFESSOR" role can access this endpoint.
        * Accepts multipart/form-data with the class code, quarter, year, and the PDF file.
        * Returns the saved SyllabusEntity with the extracted information.
     */

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyllabusEntity> importSyllabusPdf(Authentication auth, @RequestParam("classCode") String classCode, @RequestParam("quarter") String quarter, @RequestParam("year") String year, @RequestParam("file") MultipartFile file) {
        
        requireProfessor(auth);
        SyllabusEntity saved = syllabusImportService.importPdfAndUpsert(classCode, quarter, year, file);
        return ResponseEntity.ok(saved);
    }

    /*
        * Endpoint to fetch a syllabus for a specific class.
        * Only authenticated users who are enrolled in or teach the class can access it.
        * Returns 404 if the syllabus hasn't been uploaded yet.
     */
    @GetMapping("")
    public ResponseEntity<SyllabusEntity> getSyllabusForClass(Authentication auth,@RequestParam("classCode") String classCode,@RequestParam("quarter") String quarter, @RequestParam("year") String year) {
        UserAccountEntity user = requireAuthenticatedUser(auth);

        boolean teaches = teachesRelationRepository.existsByUserIdAndClassCodeAndQuarterAndYear(user.getUserId(), classCode, quarter, year);
        boolean enrolled = enrollRelationRepository.existsByUserIdAndClassCodeAndQuarterAndYear(user.getUserId(), classCode, quarter, year);

        if (!teaches && !enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        SyllabusEntity syllabus = syllabusRepository.findByClassCodeAndQuarterAndYear(classCode, quarter, year)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus not found"));

        return ResponseEntity.ok(syllabus);
    }

    /*
        * Helper method to ensure the authenticated user has the "PROFESSOR" role.
        * Throws a ResponseStatusException with HttpStatus.UNAUTHORIZED if the user is not authenticated.
        * Throws a ResponseStatusException with HttpStatus.FORBIDDEN if the user does not have the "PROFESSOR" role.
     */
    private void requireProfessor(Authentication auth) {
        UserAccountEntity user = requireAuthenticatedUser(auth);

        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        if (!role.equals("PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private UserAccountEntity requireAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String email = auth.getName().trim().toLowerCase(Locale.ROOT);
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
