package com.cscd488seniorproject.syllabussyncproject.entity;

import jakarta.persistence.*;


@Entity
public class TestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    
}
