package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.dto.SyllabusSaveRequest;
import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.SyllabusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SyllabusEntity saveOrUpdate(SyllabusSaveRequest req, String professorUserId) throws JsonProcessingException {
        String joinCode = req.courseId == null ? "" : req.courseId.trim();
        if (joinCode.isBlank()) {
            throw new IllegalArgumentException("courseId is required");
        }

        SyllabusEntity entity = syllabusRepository.findByJoinCode(joinCode)
                .orElseGet(() -> SyllabusEntity.builder()
                        .joinCode(joinCode)
                        .uploadedBy(professorUserId)
                        .build());

        entity.setClassMeetingTimes(toJson(req.classMeetingTimes));
        entity.setOfficeHours(toJson(req.officeHours));
        entity.setGradeScale(toJson(req.gradeScale));
        entity.setGradeBreakdown(toJson(req.gradeBreakdown));
        entity.setPassConditions(toJson(req.passConditions));
        entity.setAttendance(toJson(req.attendance));
        entity.setDueDates(toJson(req.dueDates));
        entity.setLateWorkPolicy(req.lateWorkPolicy);
        entity.setAiPolicy(req.aiPolicy);

        SyllabusEntity saved = syllabusRepository.save(entity);

        // Calendar generation: CourseEntity does not currently have semester start/end date
        // fields. When those columns are added (semesterStart, semesterEnd), inject
        // CourseRepository and CanvasCalendarService here and call:
        //   CanvasCalendarService.createClassSessionEvents(professorUserId, courseTitle,
        //       classMeetingTimesMap, semesterStart, semesterEnd);

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getByCourseId(String joinCode) throws JsonProcessingException {
        SyllabusEntity entity = syllabusRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("No syllabus found for course: " + joinCode));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseId", entity.getJoinCode());
        response.put("uploadedBy", entity.getUploadedBy());
        response.put("classMeetingTimes", fromJson(entity.getClassMeetingTimes()));
        response.put("officeHours", fromJson(entity.getOfficeHours()));
        response.put("gradeScale", fromJson(entity.getGradeScale()));
        response.put("gradeBreakdown", fromJson(entity.getGradeBreakdown()));
        response.put("passConditions", fromJson(entity.getPassConditions()));
        response.put("attendance", fromJson(entity.getAttendance()));
        response.put("dueDates", fromJson(entity.getDueDates()));
        response.put("lateWorkPolicy", entity.getLateWorkPolicy());
        response.put("aiPolicy", entity.getAiPolicy());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());
        return response;
    }

    private String toJson(Object value) throws JsonProcessingException {
        if (value == null) return null;
        if (value instanceof String s) return s;
        return objectMapper.writeValueAsString(value);
    }

    private Object fromJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
