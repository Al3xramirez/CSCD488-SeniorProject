package com.cscd488seniorproject.syllabussyncproject.service;

import com.cscd488seniorproject.syllabussyncproject.entity.SyllabusEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.SyllabusRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyllabusImportService {

    // This limits how much of each section we will store, just to prevent huge syllabi from causing major problems
    private static final int MAX_SECTION_CHARS = 15_000;

    // Repository for accessing syllabus data
    private final SyllabusRepository syllabusRepository;
    
    // Constructor for dependency injection
    public SyllabusImportService(SyllabusRepository syllabusRepository) {
        this.syllabusRepository = syllabusRepository;
    }

    /*
     * Imports a PDF syllabus and pulls out the 4 syllabus sections.
     * TODO: We will probably add more fields Later as this is a small extraction of data truly
     * 
     * @param classCodeRaw The raw class code
     * @param quarterRaw The raw quarter
     * @param yearRaw The raw year
     * @param pdf The PDF file
     * @return The saved SyllabusEntity
     */
    @Transactional
    public SyllabusEntity importPdfAndUpsert(String classCodeRaw, String quarterRaw, String yearRaw, MultipartFile pdf) {
        String classCode = normalizeRequired(classCodeRaw, "classCode");
        String quarter = normalizeRequired(quarterRaw, "quarter");
        String year = normalizeRequired(yearRaw, "year");

        if (pdf == null || pdf.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file required");
        }

        String extractedText = extractTextFromPdf(pdf);
        ExtractedSyllabusFields fields = extractSyllabusFields(extractedText);

        SyllabusEntity entity = syllabusRepository.findByClassCodeAndQuarterAndYear(classCode, quarter, year)
                .orElseGet(SyllabusEntity::new);

        entity.setClassCode(classCode);
        entity.setQuarter(quarter);
        entity.setYear(year);

        mergeIfPresent(entity, fields);
        return syllabusRepository.save(entity);
    }

    /*
     * Helper method to extract text from a PDF file.
     *
     * @param pdf The PDF file
     * @return The extracted text
     */
    String extractTextFromPdf(MultipartFile pdf) {
        try (InputStream is = pdf.getInputStream(); PDDocument doc = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            return stripper.getText(doc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read PDF", e);
        }
    }

    /*
     * Helper method to extract the relevant syllabus fields from the raw text.
     * Uses keyword-based wording for now, but honestly we might want to look into more advanced approaches if it doesnt work too well
     * @param rawText The raw text extracted from the PDF
     * @return The extracted syllabus fields
     */
    ExtractedSyllabusFields extractSyllabusFields(String rawText) {
        String text = normalizeExtractedText(rawText);
        String lower = text.toLowerCase(Locale.ROOT);

        List<String> gradingStart = List.of("grading scale", "grading", "assessment", "evaluation");
        List<String> attendanceStart = List.of("attendance policy", "attendance", "absences", "absentee");
        List<String> lateStart = List.of("late policy", "late work", "late", "missing work", "extensions");
        List<String> examStart = List.of("exam information", "exams", "exam", "midterm", "final", "quiz", "quizzes", "test", "tests");

        List<String> stop = union(gradingStart, attendanceStart, lateStart, examStart,
                List.of("schedule", "calendar", "assignments", "homework", "project", "projects", "textbook", "materials", "communication"));

        String grading = extractSection(text, lower, gradingStart, stop, MAX_SECTION_CHARS);
        String attendance = extractSection(text, lower, attendanceStart, stop, MAX_SECTION_CHARS);
        String late = extractSection(text, lower, lateStart, stop, MAX_SECTION_CHARS);
        String exams = extractSection(text, lower, examStart, stop, MAX_SECTION_CHARS);

        return new ExtractedSyllabusFields(grading, attendance, late, exams);
    }

    /*
     * Helper method to merge the extracted syllabus fields into the entity if they are present.
     *
     * @param entity The syllabus entity
     * @param fields The extracted syllabus fields
     */
    private static void mergeIfPresent(SyllabusEntity entity, ExtractedSyllabusFields fields) {
        if (entity == null || fields == null) return;

        if (isMeaningful(fields.gradingScale())) {
            entity.setGradingScale(fields.gradingScale());
        }
        if (isMeaningful(fields.attendancePolicy())) {
            entity.setAttendancePolicy(fields.attendancePolicy());
        }
        if (isMeaningful(fields.latePolicy())) {
            entity.setLatePolicy(fields.latePolicy());
        }
        if (isMeaningful(fields.examInfo())) {
            entity.setExamInfo(fields.examInfo());
        }
    }

    /*
     * Helper method to extract a section of text based on start and stop keywords.
     *
     * @param text The original text
     * @param lower The lowercase version of the text
     * @param startKeywords The keywords indicating the start of the section
     * @param stopKeywords The keywords indicating the end of the section
     * @param maxChars The maximum number of characters to extract
     * @return The extracted section, or null if not found
     */
    private static String extractSection(String text, String lower, List<String> startKeywords, List<String> stopKeywords, int maxChars) {
        int start = findFirstKeywordIndex(lower, startKeywords);
        if (start < 0) return null;

        int contentStart = start;
        int nextNewline = text.indexOf('\n', start);
        if (nextNewline > start && nextNewline - start <= 200) {
            contentStart = nextNewline + 1;
        }

        int end = findFirstKeywordIndexAfter(lower, stopKeywords, contentStart);
        if (end < 0) {
            end = text.length();
        }

        if (end <= contentStart) return null;

        String section = text.substring(contentStart, end).trim();
        if (!isMeaningful(section)) return null;

        if (maxChars > 0 && section.length() > maxChars) {
            section = section.substring(0, maxChars).trim();
        }

        return section;
    }

    /*
     * Helper method to find the first occurrence of any keyword in the given list.
     *
     * @param lower The lowercase version of the text
     * @param keywords The list of keywords to search for
     * @return The index of the first occurrence, or -1 if not found
     */
    private static int findFirstKeywordIndex(String lower, List<String> keywords) {
        int best = -1;
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            int idx = lower.indexOf(kw.toLowerCase(Locale.ROOT));
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    /*
     * Helper method to find the first occurrence of any keyword in the given list after a specified index.
     *
     * @param lower The lowercase version of the text
     * @param keywords The list of keywords to search for
     * @param afterIndex The index after which to start the search
     * @return The index of the first occurrence, or -1 if not found
     */
    private static int findFirstKeywordIndexAfter(String lower, List<String> keywords, int afterIndex) {
        int best = -1;
        int from = Math.max(0, afterIndex);
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            int idx = lower.indexOf(kw.toLowerCase(Locale.ROOT), from);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    /*
     * Helper method to normalize the extracted text by standardizing newlines, tabs, and multiple spaces.
     *
     * @param raw The raw extracted text
     * @return The normalized text
     */
    private static String normalizeExtractedText(String raw) {
        String s = Objects.toString(raw, "");
        s = s.replace("\r\n", "\n").replace('\r', '\n');
        s = s.replaceAll("[\\t\\f\\u000B]+", " ");
        s = s.replaceAll("[ ]{2,}", " ");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    /*
     * Helper method to determine if a string is meaningful (not null, not blank, and at least 5 characters).
     * We might need to modify the criteria for what counts as meaningful as we test it with real syllabus data
     * @param s The string to check
     * @return True if the string is meaningful, false otherwise
     */
    private static boolean isMeaningful(String s) {
        return s != null && !s.trim().isBlank() && s.trim().length() >= 5;
    }

    /*
     * Helper method to normalize a required field by trimming and validating it.
     *
     * @param value The value to normalize
     * @param fieldName The name of the field
     * @return The normalized value
     * @throws ResponseStatusException if the value is blank
     */
    private static String normalizeRequired(String value, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
        }
        return v;
    }

    /*
     * Helper method to combine multiple lists into a single list.
     *
     * @param lists The lists to combine
     * @return The combined list
     */
    @SafeVarargs
    private static List<String> union(List<String>... lists) {
        List<String> out = new ArrayList<>();
        if (lists == null) return out;
        for (List<String> l : lists) {
            if (l == null) continue;
            out.addAll(l);
        }
        return out;
    }
    // Record to hold the extracted syllabus fields
    record ExtractedSyllabusFields(String gradingScale, String attendancePolicy, String latePolicy, String examInfo) {
    }
}
