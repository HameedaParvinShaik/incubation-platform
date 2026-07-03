package com.startupincubator.repository;

import com.startupincubator.entity.Startup;
import com.startupincubator.enums.StartupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StartupRepository extends JpaRepository<Startup, Long> {

    // ✅ Use this instead of findByFounderId
    List<Startup> findByUserId(Long userId);
    
    List<Startup> findByStatus(StartupStatus status);
    List<Startup> findByIsApprovedTrue();
    
    // ✅ Find startups by mentor ID
    List<Startup> findByMentorId(Long mentorId);
    
    // ✅ Find startups where mentor is not assigned
    List<Startup> findByMentorIdIsNull();
    
    // ❌ REMOVE THIS - No founderId field exists
    // Optional<Startup> findByFounderId(Long founderId);
    
    long countByStatus(StartupStatus status);
    long countByIsApprovedTrue();
    
    @Query("SELECT s FROM Startup s ORDER BY s.createdAt DESC")
    List<Startup> findRecentStartups();
    
    List<Startup> findByNameContainingIgnoreCase(String name);
    
    List<Startup> findTop10ByOrderByCreatedAtDesc();
    
    @Query("SELECT s FROM Startup s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Startup> searchStartups(@Param("keyword") String keyword);
}