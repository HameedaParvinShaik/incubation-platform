package com.startupincubator.service.impl;

import com.startupincubator.entity.Startup;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.exception.ResourceNotFoundException;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.service.StartupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {

    private final StartupRepository startupRepository;

    @Override
    public Startup createStartup(Startup startup) {
        // ✅ Set defaults if not set
        if (startup.getStatus() == null) {
            startup.setStatus(StartupStatus.PENDING);
        }
        if (startup.getIsApproved() == null) {
            startup.setIsApproved(false);
        }
        startup.setCreatedAt(LocalDateTime.now());
        startup.setUpdatedAt(LocalDateTime.now());
        return startupRepository.save(startup);
    }

    @Override
    public Optional<Startup> findById(Long id) {
        return startupRepository.findById(id);
    }

    @Override
    public List<Startup> findAllStartups() {
        return startupRepository.findAll();
    }

    @Override
    public List<Startup> findByUserId(Long userId) {
        return startupRepository.findByUserId(userId);
    }

    @Override
    public List<Startup> findByStatus(StartupStatus status) {
        return startupRepository.findByStatus(status);
    }

    @Override
    public Startup updateStartup(Startup startup) {
        startup.setUpdatedAt(LocalDateTime.now());
        return startupRepository.save(startup);
    }

    @Override
    public void deleteStartup(Long id) {
        startupRepository.deleteById(id);
    }

    @Override
    public long countStartups() {
        return startupRepository.count();
    }

    @Override
    public List<Startup> findApprovedStartups() {
        return startupRepository.findByIsApprovedTrue();
    }

    @Override
    public List<Startup> findRecentStartups() {
        return startupRepository.findRecentStartups();
    }

    @Override
    public List<Startup> searchStartups(String keyword) {
        return startupRepository.searchStartups(keyword);
    }
}