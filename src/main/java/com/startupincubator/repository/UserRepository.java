package com.startupincubator.repository;

import com.startupincubator.entity.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ FIXED: Use LEFT JOIN FETCH to ensure roles are loaded
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);
   long countByIsActiveTrue();
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
     // ✅ Eagerly load roles
    @EntityGraph(attributePaths = {"roles"})
    List<User> findAll();
    
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findById(Long id);
}