package com.startupincubator.repository;

import com.startupincubator.entity.Mentor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorRepository extends JpaRepository<Mentor, Long> {

    Optional<Mentor> findByUserId(Long userId);
    List<Mentor> findByAvailableTrue();
    List<Mentor> findByExpertiseContaining(String expertise);
    
    long countByAvailableTrue();
    
    @Query("SELECT AVG(m.rating) FROM Mentor m")
    Double getAverageRating();

    // ✅ ADD THIS - Find available mentors with capacity
    @Query("SELECT m FROM Mentor m WHERE m.available = true AND (m.currentStartups IS NULL OR m.currentStartups < m.maxStartups)")
    List<Mentor> findAvailableMentors();
}