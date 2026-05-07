package com.cscd488seniorproject.syllabussyncproject.repository;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    boolean existsByEmail(String email);

    Optional<UserAccountEntity> findByEmail(String email);

    @Query(value = "SELECT u.* FROM useraccount u " +
           "INNER JOIN class_members cm ON u.UserID = cm.UserID " +
           "WHERE cm.ClassID = :classId", nativeQuery = true)
    List<UserAccountEntity> findClassMembers(@Param("classId") Long classId);
}
