package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.dto.EnrollmentDTO;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentId;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.ClassEnrollmentRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.CourseRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private ClassEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private UserAccountEntity testUser;
    private CourseEntity testCourse;

    @BeforeEach
    void setUp() {
        testUser = new UserAccountEntity();
        testUser.setUserId("user-001");
        testUser.setEmail("user@example.com");

        testCourse = new CourseEntity("CS101", "FALL", 2024, "Intro to CS");
    }

    @Test
    void testEnrollUserInCourse_Success() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(courseRepository.findByClassCodeAndQuarterAndYear("CS101", "FALL", 2024))
            .thenReturn(Optional.of(testCourse));
        when(enrollmentRepository.existsByUserIdAndClassCode("user-001", "CS101"))
            .thenReturn(false);
        when(enrollmentRepository.save(any(ClassEnrollmentEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentDTO result = enrollmentService.enrollUserInCourse("user-001", "CS101", "FALL", 2024);

        assertNotNull(result);
        assertEquals("user-001", result.getUserId());
        assertEquals("CS101", result.getClassCode());
        verify(enrollmentRepository, times(1)).save(any());
    }

    @Test
    void testEnrollUserInCourse_UserNotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            enrollmentService.enrollUserInCourse("nonexistent", "CS101", "FALL", 2024)
        );
    }

    @Test
    void testEnrollUserInCourse_AlreadyEnrolled() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(courseRepository.findByClassCodeAndQuarterAndYear("CS101", "FALL", 2024))
            .thenReturn(Optional.of(testCourse));
        when(enrollmentRepository.existsByUserIdAndClassCode("user-001", "CS101"))
            .thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            enrollmentService.enrollUserInCourse("user-001", "CS101", "FALL", 2024)
        );
    }

    @Test
    void testIsUserEnrolled() {
        when(enrollmentRepository.existsByUserIdAndClassCode("user-001", "CS101"))
            .thenReturn(true);

        boolean result = enrollmentService.isUserEnrolled("user-001", "CS101");
        assertTrue(result);
    }

    @Test
    void testGetUserEnrollments() {
        ClassEnrollmentId id = new ClassEnrollmentId("user-001", "CS101", "FALL", 2024);
        ClassEnrollmentEntity enrollment = new ClassEnrollmentEntity(id);
        enrollment.setCourseEntity(testCourse);

        when(enrollmentRepository.findByUserId("user-001"))
            .thenReturn(Arrays.asList(enrollment));

        List<EnrollmentDTO> result = enrollmentService.getUserEnrollments("user-001");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getClassCode());
    }

    @Test
    void testUnenrollUserFromCourse() {
        enrollmentService.unenrollUserFromCourse("user-001", "CS101");
        verify(enrollmentRepository, times(1)).deleteByUserIdAndClassCode("user-001", "CS101");
    }

    @Test
    void testGetCourseEnrollmentCount() {
        when(enrollmentRepository.countByClassCode("CS101")).thenReturn(25L);

        long count = enrollmentService.getCourseEnrollmentCount("CS101");
        assertEquals(25L, count);
    }
}