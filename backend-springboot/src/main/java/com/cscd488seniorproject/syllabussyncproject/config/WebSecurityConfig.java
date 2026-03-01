package com.cscd488seniorproject.syllabussyncproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/* This class is used to configure Spring Security, 
which is the security framework we are using for authentication and authorization */

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		// @formatter:off
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers(
					"/", "/index.html", "/favicon.ico",
        			"/assets/**", "/css/**", "/js/**")
				.permitAll()
				.requestMatchers( "/login", "/signup",
                    "/api/login", "/api/signup", "/api/check-auth",
                    "/api/health")
				.permitAll().anyRequest().authenticated()
			)
			.formLogin((form) -> form
    			.loginProcessingUrl("/api/login")
    			.successHandler((req, res, auth) -> res.setStatus(200))
  		  		.failureHandler((req, res, ex) -> res.sendError(401))
			)
			.logout(LogoutConfigurer::permitAll);
		// @formatter:on

		return http.build();
	}

	/*This bean is used to encode the password using BCrypt, 
	which is a hashing algorithm that is used to store passwords securely in the database*/
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}