package com.cscd488seniorproject.syllabussyncproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentDTO {
    private String userId;
    private String classCode;
    private String quarter;
    private Integer year;
    private String className;
}