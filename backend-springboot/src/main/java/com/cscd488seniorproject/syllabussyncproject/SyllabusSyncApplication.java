package com.cscd488seniorproject.syllabussyncproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyllabusSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyllabusSyncApplication.class, args);
	}
}
