package com.cscd488seniorproject.syllabussyncproject.controllers;

import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursExceptionDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursScheduleDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursViewDTO;
import com.cscd488seniorproject.syllabussyncproject.service.OfficeHoursService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/office-hours")
public class OfficeHoursController {

    private final OfficeHoursService officeHoursService;

    public OfficeHoursController(OfficeHoursService officeHoursService) {
        this.officeHoursService = officeHoursService;
    }

    // ── TA / Professor: manage own schedule ───────────────────────────────────

    @GetMapping("/me/schedule")
    public ResponseEntity<List<OfficeHoursScheduleDTO>> getMySchedule(Authentication auth) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(officeHoursService.getMySchedule(email));
    }

    @PostMapping("/me/schedule")
    public ResponseEntity<OfficeHoursScheduleDTO> addScheduleBlock(Authentication auth,
                                                                    @RequestBody OfficeHoursScheduleDTO req) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(officeHoursService.addScheduleBlock(email, req));
    }

    @DeleteMapping("/me/schedule/{id}")
    public ResponseEntity<Void> deleteScheduleBlock(Authentication auth, @PathVariable Long id) {
        String email = auth == null ? null : auth.getName();
        officeHoursService.deleteScheduleBlock(email, id);
        return ResponseEntity.noContent().build();
    }

    // ── TA / Professor: manage own exceptions ─────────────────────────────────

    @GetMapping("/me/exceptions")
    public ResponseEntity<List<OfficeHoursExceptionDTO>> getMyExceptions(Authentication auth) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(officeHoursService.getMyExceptions(email));
    }

    @PostMapping("/me/exceptions")
    public ResponseEntity<OfficeHoursExceptionDTO> addException(Authentication auth,
                                                                 @RequestBody OfficeHoursExceptionDTO req) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(officeHoursService.addException(email, req));
    }

    @DeleteMapping("/me/exceptions/{id}")
    public ResponseEntity<Void> deleteException(Authentication auth, @PathVariable Long id) {
        String email = auth == null ? null : auth.getName();
        officeHoursService.deleteException(email, id);
        return ResponseEntity.noContent().build();
    }

    // ── Student: view a TA or Professor's hours ───────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<OfficeHoursViewDTO> getUserOfficeHours(Authentication auth,
                                                                  @PathVariable String userId) {
        String email = auth == null ? null : auth.getName();
        return ResponseEntity.ok(officeHoursService.getUserOfficeHours(email, userId));
    }
}
