package com.cscd488seniorproject.syllabussyncproject.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;

/*  This interface is used to interact with the UserAccount table in the database using Spring Data JPA, 
 which provides built-in methods for common database operations such as findById, save, delete, etc.
Kind of sucks that we have to write a repo interface for each entity but very useful*/

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    /*  These are custom query methods that we can use to find a user by their email,
    and to check if a user with a given email already exists in the database */
    Optional<UserAccountEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<UserAccountEntity> findByCanvasIcalUrlEncryptedIsNotNull();
    
}
