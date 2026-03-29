package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.dto.ClassSummaryDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.CreateClassRequestDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.JoinClassRequestDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.StudentSummaryDTO;
import com.cscd488seniorproject.syllabussyncproject.service.CourseService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class ClassesController {

    private final CourseService courseService;

    public ClassesController(CourseService courseService) {
        this.courseService = courseService;
    }

    // Get all classes the user is teaching or enrolled in
    @GetMapping("/mine")
    public ResponseEntity<List<ClassSummaryDTO>> mine(Authentication auth) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(courseService.getMyClasses(email));
    }

    // Create a new class (professors only)
    @PostMapping("")
    public ResponseEntity<ClassSummaryDTO> create(Authentication auth, @RequestBody CreateClassRequestDTO req) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(courseService.createClass(email, req));
    }

    // Join a class using a join code (students only)
    @PostMapping("/join")
    public ResponseEntity<ClassSummaryDTO> join(Authentication auth, @RequestBody JoinClassRequestDTO req) {
        String email = auth == null ? null : auth.getName();
        String joinCode = req == null ? null : req.joinCode;
        return ResponseEntity.ok(courseService.joinClassByCode(email, joinCode));
    }
    
    // Get the roster of a class by its join code (professors only)
    @GetMapping("/{joinCode}/students")
    public ResponseEntity<List<StudentSummaryDTO>> students(Authentication auth, @PathVariable String joinCode) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(courseService.getRosterByJoinCode(email, joinCode));
    }

    // Delete a class by its join code (professors only)
    @PostMapping("/delete")
    public ResponseEntity<ClassSummaryDTO> delete(Authentication auth, @RequestBody JoinClassRequestDTO req) {

        String email = auth == null ? null : auth.getName();
        String joinCode = req == null ? null : req.joinCode;
        return ResponseEntity.ok(courseService.deleteClassByJoinCode(email, joinCode));
    }
}
