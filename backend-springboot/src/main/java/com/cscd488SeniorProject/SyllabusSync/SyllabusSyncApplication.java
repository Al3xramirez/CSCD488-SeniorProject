package com.cscd488SeniorProject.SyllabusSync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication (exclude = {org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class}) // Exclude DB configuration for now 
public class SyllabusSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyllabusSyncApplication.class, args);
	}

}
