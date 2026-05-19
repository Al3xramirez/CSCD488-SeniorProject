package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.ClassEnrollmentId;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClassEnrollmentRepositoryTest {

    @Autowired
    private ClassEnrollmentRepository enrollmentRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    private UserAccountEntity testUser;
    private CourseEntity testCourse;
    private ClassEnrollmentEntity testEnrollment;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserAccountEntity();
        testUser.setUserId("test-user-001");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole("STUDENT");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        userRepository.save(testUser);

        // Create test course
        testCourse = new CourseEntity("CS101", "FALL", 2024, "Introduction to CS");
        testCourse.setJoinCode("JOIN123");
        courseRepository.save(testCourse);

        // Create test enrollment
        ClassEnrollmentId enrollmentId = new ClassEnrollmentId("test-user-001", "CS101", "FALL", 2024);
        testEnrollment = new ClassEnrollmentEntity(enrollmentId);
        testEnrollment.setUserEntity(testUser);
        testEnrollment.setCourseEntity(testCourse);
        enrollmentRepository.save(testEnrollment);
    }

    @Test
    void testFindByUserId() {
        List<ClassEnrollmentEntity> enrollments = enrollmentRepository.findByUserId("test-user-001");
        assertFalse(enrollments.isEmpty());
        assertEquals(1, enrollments.size());
        assertEquals("CS101", enrollments.get(0).getClassCode());
    }

    @Test
    void testFindByClassCode() {
        List<ClassEnrollmentEntity> enrollments = enrollmentRepository.findByClassCode("CS101");
        assertFalse(enrollments.isEmpty());
        assertEquals(1, enrollments.size());
        assertEquals("test-user-001", enrollments.get(0).getUserId());
    }

    @Test
    void testFindByUserIdAndClassCode() {
        Optional<ClassEnrollmentEntity> enrollment = enrollmentRepository
            .findByUserIdAndClassCode("test-user-001", "CS101");
        assertTrue(enrollment.isPresent());
        assertEquals("FALL", enrollment.get().getQuarter());
        assertEquals(2024, enrollment.get().getYear());
    }

    @Test
    void testExistsByUserIdAndClassCode() {
        boolean exists = enrollmentRepository.existsByUserIdAndClassCode("test-user-001", "CS101");
        assertTrue(exists);

        boolean notExists = enrollmentRepository.existsByUserIdAndClassCode("nonexistent", "CS101");
        assertFalse(notExists);
    }

    @Test
    void testFindByClassCodeAndQuarterAndYear() {
        List<ClassEnrollmentEntity> enrollments = enrollmentRepository
            .findByClassCodeAndQuarterAndYear("CS101", "FALL", 2024);
        assertEquals(1, enrollments.size());
        assertEquals("test-user-001", enrollments.get(0).getUserId());
    }

    @Test
    void testCountByClassCode() {
        long count = enrollmentRepository.countByClassCode("CS101");
        assertEquals(1, count);
    }

    @Test
    void testDeleteByUserIdAndClassCode() {
        enrollmentRepository.deleteByUserIdAndClassCode("test-user-001", "CS101");
        Optional<ClassEnrollmentEntity> deleted = enrollmentRepository
            .findByUserIdAndClassCode("test-user-001", "CS101");
        assertTrue(deleted.isEmpty());
    }
}