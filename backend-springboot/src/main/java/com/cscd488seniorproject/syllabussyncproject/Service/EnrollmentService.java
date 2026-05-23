package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.dto.EnrollmentDTO;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentId;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.ClassEnrollmentRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.CourseRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final ClassEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userRepository;

    /**
     * Enroll a user in a course
     */
    public EnrollmentDTO enrollUserInCourse(String userId, String classCode, String quarter, Integer year) {
        // Verify user exists
        UserAccountEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify course exists
        CourseEntity course = courseRepository.findByClassCodeAndQuarterAndYear(classCode, quarter, year)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndClassCode(userId, classCode)) {
            throw new IllegalStateException("User already enrolled in this course");
        }

        // Create enrollment
        ClassEnrollmentId id = new ClassEnrollmentId(userId, classCode, quarter, year);
        ClassEnrollmentEntity enrollment = new ClassEnrollmentEntity(id);
        enrollment.setUserEntity(user);
        enrollment.setCourseEntity(course);

        ClassEnrollmentEntity saved = enrollmentRepository.save(enrollment);
        return mapToDTO(saved);
    }

    /**
     * Get all courses for a user
     */
    public List<EnrollmentDTO> getUserEnrollments(String userId) {
        return enrollmentRepository.findByUserId(userId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get all students enrolled in a course
     */
    public List<EnrollmentDTO> getCourseEnrollments(String classCode) {
        return enrollmentRepository.findByClassCode(classCode).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Check if user is enrolled in course
     */
    public boolean isUserEnrolled(String userId, String classCode) {
        return enrollmentRepository.existsByUserIdAndClassCode(userId, classCode);
    }

    /**
     * Remove user from course
     */
    public void unenrollUserFromCourse(String userId, String classCode) {
        enrollmentRepository.deleteByUserIdAndClassCode(userId, classCode);
    }

    /**
     * Get enrollment count for a course
     */
    public long getCourseEnrollmentCount(String classCode) {
        return enrollmentRepository.countByClassCode(classCode);
    }

    /**
     * Map entity to DTO
     */
    private EnrollmentDTO mapToDTO(ClassEnrollmentEntity entity) {
        return EnrollmentDTO.builder()
            .userId(entity.getUserId())
            .classCode(entity.getClassCode())
            .quarter(entity.getQuarter())
            .year(entity.getYear())
            .className(entity.getCourseEntity() != null ? entity.getCourseEntity().getTitle() : null)
            .build();
    }
}