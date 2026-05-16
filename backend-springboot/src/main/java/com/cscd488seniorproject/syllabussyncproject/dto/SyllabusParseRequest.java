package com.cscd488seniorproject.syllabussyncproject.dto;

public class SyllabusParseRequest {
    /** The course join code used as the course identifier. */
    public String courseId;
    /** Raw PDF bytes encoded as a Base64 string. */
    public String pdfBase64;
}
