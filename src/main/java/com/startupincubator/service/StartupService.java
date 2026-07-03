package com.startupincubator.service;

import com.startupincubator.entity.Startup;
import com.startupincubator.enums.StartupStatus;
import java.util.List;
import java.util.Optional;

public interface StartupService {

    Startup createStartup(Startup startup);
    Optional<Startup> findById(Long id);
    List<Startup> findAllStartups();
    List<Startup> findByUserId(Long userId);
    List<Startup> findByStatus(StartupStatus status);
    Startup updateStartup(Startup startup);
    void deleteStartup(Long id);
    
    // ✅ ADD THESE METHODS
    long countStartups();
    List<Startup> findApprovedStartups();
    List<Startup> findRecentStartups();
    List<Startup> searchStartups(String keyword);
}