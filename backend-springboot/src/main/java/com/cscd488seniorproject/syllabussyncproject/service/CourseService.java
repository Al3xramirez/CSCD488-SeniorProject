package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.dto.ClassSummaryDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.CourseDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.CreateClassRequestDTO;
import com.cscd488seniorproject.syllabussyncproject.dto.StudentSummaryDTO;
import com.cscd488seniorproject.syllabussyncproject.entity.CourseEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.EnrollRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TARelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.TeachesRelationEntity;
import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.CourseRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.EnrollRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TARelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.TeachesRelationRepository;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseService {

    // Payload for returning profile photos from endpoints
    public record PhotoPayload(byte[] bytes, String contentType) {}

    // Configuration for join code generation
    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 5;
    private static final int JOIN_CODE_MAX_ATTEMPTS = 20;

    // Repositories for accessing the database
    private final CourseRepository courseRepo;
    private final TeachesRelationRepository teachesRepo;
    private final EnrollRelationRepository enrollRepo;
    private final TARelationRepository taRepo;
    private final UserAccountRepository userRepo;

    // This is for generating random join codes for courses
    private final SecureRandom random = new SecureRandom();

    // Constructor for dependency injection of repositories
    public CourseService(CourseRepository courseRepo, TeachesRelationRepository teachesRepo, EnrollRelationRepository enrollRepo, TARelationRepository taRepo, UserAccountRepository userRepo) {
        this.courseRepo = courseRepo;
        this.teachesRepo = teachesRepo;
        this.enrollRepo = enrollRepo;
        this.taRepo = taRepo;
        this.userRepo = userRepo;
    }

    // getMyClasses returns a list of classes that the user is either teaching or enrolled in, based on their role
    public List<ClassSummaryDTO> getMyClasses(String email) {

        // me is the UserAccountEntity for the current auth. user, requiredUserByEmail grabs the user from the database and throws if not found
        UserAccountEntity me = requireUserByEmail(email);
        // role is normalized to uppercase and getRole returns the role of the user (e.g. "PROFESSOR", "STUDENT", "TA")
        String role = normalizeRole(me.getRole());

        // Depending on the role, we either find courses taught by the user, assigned as TA, or enrolled
        List<CourseEntity> courses;
        if (role.equals("PROFESSOR")) {
            courses = courseRepo.findCoursesTaughtBy(me.getUserId());
        } else if (role.equals("TA")) {
            courses = courseRepo.findCoursesTAedBy(me.getUserId());
        } else {
            courses = courseRepo.findCoursesEnrolledBy(me.getUserId());
        }
        //returns a list of ClassSummaryDTOs 
        return courses.stream()
                .map(c -> new ClassSummaryDTO(c.getClassCode(), c.getQuarter(), c.getYear(), c.getTitle(), c.getJoinCode()))
                .toList();
    }

    /* createClass allows a professor to create a new class with the specified details. 
       It checks for conflicts and generates a unique join code. 
       Parameters are email from the user, and req which contains the class details.
       Professor only class*/
    public ClassSummaryDTO createClass(String email, CreateClassRequestDTO req) {

        /* same setup as getMyClasses to get the current user and check their role, 
         this role is required to be "PROFESSOR" to create a class */
        UserAccountEntity me = requireUserByEmail(email);
        requireRole(me, Set.of("PROFESSOR"));

        // Normalize and validate the input parameters from the request DTO, ensuring they are not blank
        String classCode = normalizeRequired(req == null ? null : req.classCode, "classCode");
        String quarter = normalizeRequired(req == null ? null : req.quarter, "quarter");
        Integer year = normalizeRequiredInt(req == null ? null : req.year, "year");
        String title = normalizeRequired(req == null ? null : req.title, "title");

        //Optional is used to handle the possibility that a course with the same class code, quarter, and year already exists.
        // If such a course exists, a 409 Conflict response is thrown to indicate that the course cannot be created. 
        // Lowkey didnt know that a 409 was a thing
        Optional<CourseEntity> existing = courseRepo.findByClassCodeAndQuarterAndYear(classCode, quarter, year);
        if (existing.isPresent()) {
            CourseEntity existingCourse = existing.get();
            boolean alreadyTeaches = teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                me.getUserId(), classCode, quarter, year);
            if (alreadyTeaches) {
                return new ClassSummaryDTO(existingCourse.getClassCode(), existingCourse.getQuarter(),
                    existingCourse.getYear(), existingCourse.getTitle(), existingCourse.getJoinCode());
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course already exists for that ClassCode/Quarter/Year");
        }
        // joinCode simply generates a unique join code for the new course
        String joinCode = generateUniqueJoinCode();

        // A new CourseEntity is created and populated with the provided details and the generated join code, then saved to the database
        CourseEntity course = new CourseEntity();
        course.setClassCode(classCode);
        course.setQuarter(quarter);
        course.setYear(year);
        course.setTitle(title);
        course.setJoinCode(joinCode);
        courseRepo.save(course);

        // A new TeachesRelationEntity is created to link the professor to the course they just created, and saved to the database
        TeachesRelationEntity teaches = new TeachesRelationEntity();
        teaches.setUserId(me.getUserId());
        teaches.setClassCode(classCode);
        teaches.setQuarter(quarter);
        teaches.setYear(year);
        teachesRepo.save(teaches);
        // Finally, a ClassSummaryDTO is returned with the details of the newly created course
        return new ClassSummaryDTO(classCode, quarter, year, title, joinCode);
    }
    // joinClassByCode allows a student to join a class using a join code. TAs are assigned directly by professors.
    public ClassSummaryDTO joinClassByCode(String email, String joinCodeRaw) {
        UserAccountEntity me = requireUserByEmail(email);
        requireRole(me, Set.of("STUDENT"));

        // Normalize and validate the join code input, ensuring it is not blank
        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");
        CourseEntity course = courseRepo.findByJoinCode(joinCode).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        // Check if the user is already enrolled in the course, and if so, throw a 409 Conflict response
        boolean already = enrollRepo.existsByUserIdAndClassCodeAndQuarterAndYear(me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (already) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        }
        // Once all checks are passed, a new EnrollRelationEntity is created to link the user to the course they are joining, saving to the db
        EnrollRelationEntity enroll = new EnrollRelationEntity();
        enroll.setUserId(me.getUserId());
        enroll.setClassCode(course.getClassCode());
        enroll.setQuarter(course.getQuarter());
        enroll.setYear(course.getYear());
        enrollRepo.save(enroll);
        // Again, return a ClassSummaryDTO with the details of the course that was joined
        return new ClassSummaryDTO(course.getClassCode(), course.getQuarter(), course.getYear(), course.getTitle(), course.getJoinCode());
    }
    
    // getRosterByJoinCode allows a professor to retrieve the roster of students enrolled in a class using the class's join code. 
    // It checks if the user is a professor for that class and then returns a list of StudentSummaryDTOs representing the enrolled students.
    public List<StudentSummaryDTO> getRosterByJoinCode(String email, String joinCodeRaw) {
        UserAccountEntity me = requireUserByEmail(email);
        requireRole(me, Set.of("PROFESSOR"));

        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");
        CourseEntity course = courseRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        boolean teaches = teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (!teaches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view roster for this class");
        }

        // Fetch enrolled students from enrollsrelation
        List<EnrollRelationEntity> enrollments = enrollRepo.findAllByClassCodeAndQuarterAndYear(
                course.getClassCode(), course.getQuarter(), course.getYear());
        List<String> studentIds = enrollments.stream().map(EnrollRelationEntity::getUserId).distinct().toList();

        List<StudentSummaryDTO> result = new ArrayList<>();

        if (!studentIds.isEmpty()) {
            userRepo.findAllById(studentIds).forEach(u ->
                result.add(new StudentSummaryDTO(u.getUserId(), u.getEmail(), u.getFirstName(), u.getLastName(), "STUDENT"))
            );
        }
        result.addAll(fetchTASummaries(course.getClassCode(), course.getQuarter(), course.getYear()));
        return result;
    }

    // getTAsForCourse returns the list of TAs assigned to a course. Accessible by anyone enrolled in or teaching the course.
    public List<StudentSummaryDTO> getTAsForCourse(String email, String joinCodeRaw) {
        UserAccountEntity me = requireUserByEmail(email);
        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");
        CourseEntity course = courseRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        boolean hasAccess =
                enrollRepo.existsByUserIdAndClassCodeAndQuarterAndYear(me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear()) ||
                taRepo.existsByUserIdAndClassCodeAndQuarterAndYear(me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear()) ||
                teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this class");
        }

        return fetchTASummaries(course.getClassCode(), course.getQuarter(), course.getYear());
    }

    private List<StudentSummaryDTO> fetchTASummaries(String classCode, String quarter, Integer year) {
        List<String> taIds = taRepo.findAllByClassCodeAndQuarterAndYear(classCode, quarter, year)
                .stream().map(TARelationEntity::getUserId).distinct().toList();
        if (taIds.isEmpty()) return List.of();
        return userRepo.findAllById(taIds).stream()
                .map(u -> new StudentSummaryDTO(u.getUserId(), u.getEmail(), u.getFirstName(), u.getLastName(), "TA"))
                .toList();
    }

    // assignTA allows a professor to assign a TA to their course by the TA's email address.
    public StudentSummaryDTO assignTA(String profEmail, String joinCodeRaw, String taEmailRaw) {
        UserAccountEntity prof = requireUserByEmail(profEmail);
        requireRole(prof, Set.of("PROFESSOR"));

        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");
        CourseEntity course = courseRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        boolean teaches = teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                prof.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (!teaches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to manage TAs for this class");
        }

        String taEmail = normalizeRequired(taEmailRaw, "taEmail").toLowerCase(Locale.ROOT);
        UserAccountEntity ta = userRepo.findByEmail(taEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with that email"));

        if (normalizeRole(ta.getRole()).equals("PROFESSOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign a professor as TA");
        }

        boolean already = taRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                ta.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (already) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a TA for this class");
        }

        TARelationEntity rel = new TARelationEntity();
        rel.setUserId(ta.getUserId());
        rel.setClassCode(course.getClassCode());
        rel.setQuarter(course.getQuarter());
        rel.setYear(course.getYear());
        taRepo.save(rel);

        return new StudentSummaryDTO(ta.getUserId(), ta.getEmail(), ta.getFirstName(), ta.getLastName(), "TA");
    }

    // getRosterUserPhoto allows a professor to retrieve the profile photo for a user enrolled in a class.
    // It checks 1. professor role, 2. professor teaches the class, and 3. target user is enrolled.
    @Transactional(readOnly = true)
    public PhotoPayload getRosterUserPhoto(String email, String joinCodeRaw, String userIdRaw) {
        UserAccountEntity me = requireUserByEmail(email);
        requireRole(me, Set.of("PROFESSOR"));

        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");
        String userId = normalizeRequired(userIdRaw, "userId");

        CourseEntity course = courseRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        boolean teaches = teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (!teaches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view roster for this class");
        }

        boolean enrolled = enrollRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                userId, course.getClassCode(), course.getQuarter(), course.getYear());
        boolean isTa = taRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                userId, course.getClassCode(), course.getQuarter(), course.getYear());
        if (!enrolled && !isTa) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in this class");
        }

        UserAccountEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        byte[] bytes = user.getProfilePhoto();
        String ct = user.getProfilePhotoContentType();

        if (bytes == null || bytes.length == 0 || ct == null || ct.isBlank()) {
            return null;
        }
        return new PhotoPayload(bytes, ct);
    }
    
    /*Transactional ensures that all databases operations
    within a method are executed, cuz if not, we might end up in a state where the course is
    deleted but the relations are not, or vice versa*/
    @Transactional
    public ClassSummaryDTO deleteClassByJoinCode(String email, String joinCodeRaw) {

        UserAccountEntity me = requireUserByEmail(email);
        requireRole(me, Set.of("PROFESSOR"));
        String joinCode = normalizeRequired(joinCodeRaw, "joinCode");

        CourseEntity course = courseRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));
        
        boolean teaches = teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(
                me.getUserId(), course.getClassCode(), course.getQuarter(), course.getYear());
        if (!teaches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this class");
        }
        
        // Delete dependent rows first 
        //NOTE: Once we add other entities and repositories that depend on CourseEntity, we will need to delete them here
        String classCode = course.getClassCode();
        String quarter = course.getQuarter();
        Integer year = course.getYear();

        teachesRepo.deleteAllByClassCodeAndQuarterAndYear(classCode, quarter, year);
        enrollRepo.deleteAllByClassCodeAndQuarterAndYear(classCode, quarter, year);
        taRepo.deleteAllByClassCodeAndQuarterAndYear(classCode, quarter, year);
        courseRepo.delete(course);

        return new ClassSummaryDTO(course.getClassCode(), course.getQuarter(), course.getYear(), course.getTitle(), course.getJoinCode());
    }

    // ============================================================================
    // NEW FUNCTIONALITY - FROM REFACTORED SERVICE LAYER
    // ============================================================================

    /**
     * Get a course by its composite key (classCode, quarter, year)
     * @return CourseDTO or null if not found
     */
    public CourseDTO getCourseById(String classCode, String quarter, Integer year) {
        Optional<CourseEntity> course = courseRepo.findByClassCodeAndQuarterAndYear(classCode, quarter, year);
        return course.map(this::mapToDTO).orElse(null);
    }

    /**
     * Get a course by its join code
     * @return CourseDTO or null if not found
     */
    public CourseDTO getCourseByJoinCode(String joinCode) {
        Optional<CourseEntity> course = courseRepo.findByJoinCode(joinCode);
        return course.map(this::mapToDTO).orElse(null);
    }

    /**
     * Get all course instances with a specific class code
     * @return List of CourseDTOs
     */
    public List<CourseDTO> getCoursesByClassCode(String classCode) {
        return courseRepo.findByClassCode(classCode).stream()
            .map(this::mapToDTO)
            .toList();
    }

    /**
     * Get all courses taught by a specific user
     * @return List of CourseDTOs
     */
    public List<CourseDTO> getCoursesTaughtBy(String userId) {
        return courseRepo.findCoursesTaughtBy(userId).stream()
            .map(this::mapToDTO)
            .toList();
    }

    /**
     * Get all courses a user is enrolled in
     * @return List of CourseDTOs
     */
    public List<CourseDTO> getCoursesEnrolledBy(String userId) {
        return courseRepo.findCoursesEnrolledBy(userId).stream()
            .map(this::mapToDTO)
            .toList();
    }

    /**
     * Check if a course exists
     * @return true if course exists, false otherwise
     */
    public boolean courseExists(String classCode, String quarter, Integer year) {
        return courseRepo.findByClassCodeAndQuarterAndYear(classCode, quarter, year).isPresent();
    }

    /**
     * Get the enrollment count for a specific course
     * @return number of enrolled students
     */
    public long getCourseEnrollmentCount(String classCode, String quarter, Integer year) {
        return enrollRepo.countByClassCodeAndQuarterAndYear(classCode, quarter, year);
    }

    /**
     * Check if a user is enrolled in a course
     * @return true if enrolled, false otherwise
     */
    public boolean isUserEnrolled(String userId, String classCode, String quarter, Integer year) {
        return enrollRepo.existsByUserIdAndClassCodeAndQuarterAndYear(userId, classCode, quarter, year);
    }

    /**
     * Check if a user teaches a course
     * @return true if teaches, false otherwise
     */
    public boolean doesUserTeach(String userId, String classCode, String quarter, Integer year) {
        return teachesRepo.existsByUserIdAndClassCodeAndQuarterAndYear(userId, classCode, quarter, year);
    }

    /**
     * Map CourseEntity to CourseDTO for API responses
     */
    private CourseDTO mapToDTO(CourseEntity entity) {
        return CourseDTO.builder()
            .classCode(entity.getClassCode())
            .quarter(entity.getQuarter())
            .year(entity.getYear())
            .title(entity.getTitle())
            .joinCode(entity.getJoinCode())
            .build();
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    // Helper method to retrieve a user by email and throw exception if the email is not provided or the user is not found.
    private UserAccountEntity requireUserByEmail(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    // Helper method to normalize user roles and check if the user's role is in the allowed set, throwing a 403 Forbidden response if not.
    private static String normalizeRole(String role) {
        return (role == null ? "" : role.trim().toUpperCase(Locale.ROOT));
    }

    // Helper method to check if the user's role is in the allowed set of roles, throwing a 403 Forbidden response if not.
    private static void requireRole(UserAccountEntity user, Set<String> allowed) {
        String role = normalizeRole(user == null ? null : user.getRole());
        if (!allowed.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    // Helper method to normalize required string inputs, ensuring they are not null or blank, and throwing a 400 Bad Request response if validation fails.
    private static String normalizeRequired(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
        }
        return v;
    }

    //Helper method to normalize required integer inputs
    private static Integer normalizeRequiredInt(Integer value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
        }
        return value;
    }

    // This method checks if the generated join code is unique by checking the database, and if not, 
    // it retries up to a maximum number of attempts before throwing an exception.
    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_MAX_ATTEMPTS; attempt++) {
            String code = randomJoinCode();
            if (!courseRepo.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate unique join code");
    }

    // This method generates a random join code for a course.
    private String randomJoinCode() {
        StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            int idx = random.nextInt(JOIN_CODE_ALPHABET.length());
            sb.append(JOIN_CODE_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}
