package com.cscd488seniorproject.syllabussync;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		// @formatter:off
		http
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/login","/signup").permitAll()
				.requestMatchers("/**").authenticated()
			)
			.formLogin((form) -> form
				.loginPage("/login").permitAll()
    			.loginProcessingUrl("/api/login")
    			.successHandler((req, res, auth) -> res.setStatus(200))
  		  		.failureHandler((req, res, ex) -> res.sendError(401))
			)
			.logout(LogoutConfigurer::permitAll);
		// @formatter:on

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(PasswordEncoder encoder) {
		String password = encoder.encode("password");
		UserDetails user = User.withUsername("user").password(password).roles("USER").build();
		return new InMemoryUserDetailsManager(user);
	}

}