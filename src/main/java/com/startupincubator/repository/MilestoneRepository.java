package com.startupincubator.repository;

import com.startupincubator.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByStartupId(Long startupId);
    
    List<Milestone> findByStartupIdIn(List<Long> startupIds);
    
    List<Milestone> findByStartupIdAndStatus(Long startupId, String status);
    
    List<Milestone> findByMentorId(Long mentorId);
    
    long countByStartupIdAndStatus(Long startupId, String status);
    
    // ✅ ADD THIS METHOD FOR DELETE
    void deleteByStartupId(Long startupId);
}