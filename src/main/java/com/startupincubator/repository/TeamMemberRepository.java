package com.startupincubator.repository;

import com.startupincubator.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    
    List<TeamMember> findByStartupId(Long startupId);
    
    List<TeamMember> findByStartupIdAndIsActiveTrue(Long startupId);
    
    List<TeamMember> findByStartupIdAndStatus(Long startupId, String status);
}