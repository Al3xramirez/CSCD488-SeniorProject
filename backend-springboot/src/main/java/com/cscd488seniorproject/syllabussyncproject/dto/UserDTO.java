package com.cscd488seniorproject.syllabussyncproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String department;
}