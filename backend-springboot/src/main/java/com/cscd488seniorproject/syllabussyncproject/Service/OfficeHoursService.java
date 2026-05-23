package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursExceptionDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursScheduleDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.OfficeHoursViewDTO;
import com.cscd488seniorproject.syllabussyncproject.entity.OfficeHoursExceptionEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.OfficeHoursScheduleEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TARelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.OfficeHoursExceptionRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.OfficeHoursScheduleRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TARelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TeachesRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OfficeHoursService {

    private static final List<String> VALID_DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final OfficeHoursScheduleRepository scheduleRepo;
    private final OfficeHoursExceptionRepository exceptionRepo;
    private final UserAccountRepository userRepo;
    private final TARelationRepository taRepo;
    private final TeachesRelationRepository teachesRepo;

    public OfficeHoursService(OfficeHoursScheduleRepository scheduleRepo,
                               OfficeHoursExceptionRepository exceptionRepo,
                               UserAccountRepository userRepo,
                               TARelationRepository taRepo,
                               TeachesRelationRepository teachesRepo) {
        this.scheduleRepo = scheduleRepo;
        this.exceptionRepo = exceptionRepo;
        this.userRepo = userRepo;
        this.taRepo = taRepo;
        this.teachesRepo = teachesRepo;
    }

    // ── TA / Professor: own schedule ──────────────────────────────────────────

    public List<OfficeHoursScheduleDTO> getMySchedule(String email) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);
        return scheduleRepo.findAllByUserId(me.getUserId()).stream()
                .map(this::toScheduleDTO)
                .toList();
    }

    public OfficeHoursScheduleDTO addScheduleBlock(String email, OfficeHoursScheduleDTO req) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);

        String day = requireDay(req == null ? null : req.dayOfWeek);
        LocalTime start = requireTime(req == null ? null : req.startTime, "startTime");
        LocalTime end   = requireTime(req == null ? null : req.endTime,   "endTime");
        String quarter  = requireString(req == null ? null : req.quarter, "quarter");
        int year        = requireYear(req == null ? 0 : req.year);

        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        OfficeHoursScheduleEntity entity = new OfficeHoursScheduleEntity();
        entity.setUserId(me.getUserId());
        entity.setDayOfWeek(day);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setQuarter(quarter);
        entity.setYear(year);
        scheduleRepo.save(entity);

        return toScheduleDTO(entity);
    }

    public void deleteScheduleBlock(String email, Long id) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);

        if (!scheduleRepo.existsByIdAndUserId(id, me.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule block not found");
        }
        scheduleRepo.deleteById(id);
    }

    // ── TA / Professor: exceptions ────────────────────────────────────────────

    public List<OfficeHoursExceptionDTO> getMyExceptions(String email) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);
        return exceptionRepo.findAllByUserId(me.getUserId()).stream()
                .map(this::toExceptionDTO)
                .toList();
    }

    public OfficeHoursExceptionDTO addException(String email, OfficeHoursExceptionDTO req) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);

        String dateStr  = requireString(req == null ? null : req.exceptionDate, "exceptionDate");
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "exceptionDate must be YYYY-MM-DD");
        }

        LocalTime start = parseTimeOrNull(req == null ? null : req.startTime);
        LocalTime end   = parseTimeOrNull(req == null ? null : req.endTime);

        if (start != null && end != null && !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        OfficeHoursExceptionEntity entity = new OfficeHoursExceptionEntity();
        entity.setUserId(me.getUserId());
        entity.setExceptionDate(date);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setUnavailable(req == null || req.unavailable);
        entity.setNote(req == null ? null : req.note);
        exceptionRepo.save(entity);

        return toExceptionDTO(entity);
    }

    public void deleteException(String email, Long id) {
        UserAccountEntity me = requireUserByEmail(email);
        requireTAOrProfessor(me);

        if (!exceptionRepo.existsByIdAndUserId(id, me.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exception not found");
        }
        exceptionRepo.deleteById(id);
    }

    // ── Class members: view all office hours for a class ─────────────────────

    public List<OfficeHoursViewDTO> getClassOfficeHours(String classCode) {
        List<OfficeHoursViewDTO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (TARelationEntity ta : taRepo.findByClassCode(classCode)) {
            if (seen.add(ta.getUserId())) {
                userRepo.findById(ta.getUserId()).ifPresent(user -> {
                    List<OfficeHoursScheduleDTO> schedule = scheduleRepo.findAllByUserId(ta.getUserId())
                            .stream().map(this::toScheduleDTO).toList();
                    result.add(new OfficeHoursViewDTO(ta.getUserId(), user.getFirstName(),
                            user.getLastName(), "TA", schedule, List.of()));
                });
            }
        }

        for (TeachesRelationEntity prof : teachesRepo.findByClassCode(classCode)) {
            if (seen.add(prof.getUserId())) {
                userRepo.findById(prof.getUserId()).ifPresent(user -> {
                    List<OfficeHoursScheduleDTO> schedule = scheduleRepo.findAllByUserId(prof.getUserId())
                            .stream().map(this::toScheduleDTO).toList();
                    result.add(new OfficeHoursViewDTO(prof.getUserId(), user.getFirstName(),
                            user.getLastName(), "PROFESSOR", schedule, List.of()));
                });
            }
        }

        return result;
    }

    // ── Student: view a TA or Professor's hours ───────────────────────────────

    public OfficeHoursViewDTO getUserOfficeHours(String requestorEmail, String targetUserId) {
        requireUserByEmail(requestorEmail);

        String uid = targetUserId == null ? "" : targetUserId.trim();
        if (uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }

        UserAccountEntity target = userRepo.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String role = normalizeRole(target.getRole());
        if (!role.equals("TA") && !role.equals("PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That user does not have office hours");
        }

        // Return upcoming exceptions only (next 90 days) to keep the payload small
        LocalDate today = LocalDate.now();
        List<OfficeHoursScheduleDTO> schedule = scheduleRepo.findAllByUserId(uid).stream()
                .map(this::toScheduleDTO)
                .toList();
        List<OfficeHoursExceptionDTO> exceptions = exceptionRepo
                .findAllByUserIdAndExceptionDateBetween(uid, today, today.plusDays(90)).stream()
                .map(this::toExceptionDTO)
                .toList();

        return new OfficeHoursViewDTO(uid, target.getFirstName(), target.getLastName(), role, schedule, exceptions);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private OfficeHoursScheduleDTO toScheduleDTO(OfficeHoursScheduleEntity e) {
        return new OfficeHoursScheduleDTO(
                e.getId(),
                e.getDayOfWeek(),
                e.getStartTime().toString(),
                e.getEndTime().toString(),
                e.getQuarter(),
                e.getYear()
        );
    }

    private OfficeHoursExceptionDTO toExceptionDTO(OfficeHoursExceptionEntity e) {
        return new OfficeHoursExceptionDTO(
                e.getId(),
                e.getExceptionDate().toString(),
                e.getStartTime() != null ? e.getStartTime().toString() : null,
                e.getEndTime()   != null ? e.getEndTime().toString()   : null,
                e.isUnavailable(),
                e.getNote()
        );
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    private UserAccountEntity requireUserByEmail(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private static void requireTAOrProfessor(UserAccountEntity user) {
        String role = normalizeRole(user == null ? null : user.getRole());
        if (!role.equals("TA") && !role.equals("PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only TAs and Professors can manage office hours");
        }
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireString(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
        }
        return v;
    }

    private static int requireYear(int value) {
        if (value < 2000 || value > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be a valid 4-digit year");
        }
        return value;
    }

    private static String requireDay(String value) {
        String day = requireString(value, "dayOfWeek").toUpperCase(Locale.ROOT);
        if (!VALID_DAYS.contains(day)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek must be one of: " + VALID_DAYS);
        }
        return day;
    }

    private static LocalTime requireTime(String value, String fieldName) {
        String v = requireString(value, fieldName);
        try {
            return LocalTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be HH:mm");
        }
    }

    private static LocalTime parseTimeOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time must be HH:mm");
        }
    }
}
