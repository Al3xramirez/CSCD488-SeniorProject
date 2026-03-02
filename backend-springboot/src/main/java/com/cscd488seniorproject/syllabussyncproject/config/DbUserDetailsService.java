package com.cscd488seniorproject.syllabussyncproject.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;

import java.util.List;
import java.util.Locale;

/* This class is used by Spring Security to load the user details from the database, 
and to convert it into a UserDetails object that Spring Security can use for authentication and authorization */
@Component
public class DbUserDetailsService implements UserDetailsService{

    private final UserAccountRepository repo;

    public DbUserDetailsService(UserAccountRepository repo) {
        this.repo = repo;
    }

   
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String email = username.toLowerCase(Locale.ROOT);

        UserAccountEntity user = repo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );

    }
}
