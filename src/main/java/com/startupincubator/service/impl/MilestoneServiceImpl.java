package com.startupincubator.service.impl;

import com.startupincubator.entity.Milestone;
import com.startupincubator.repository.MilestoneRepository;
import com.startupincubator.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MilestoneServiceImpl implements MilestoneService {

    private final MilestoneRepository milestoneRepository;

    @Override
    public Milestone createMilestone(Milestone milestone) {
        milestone.setStatus("PENDING");
        milestone.setProgressPercentage(0);
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());
        return milestoneRepository.save(milestone);
    }

    @Override
    public Optional<Milestone> findById(Long id) {
        return milestoneRepository.findById(id);
    }

    @Override
    public List<Milestone> getMilestonesByStartup(Long startupId) {
        return milestoneRepository.findByStartupId(startupId);
    }

    @Override
    public List<Milestone> getMilestonesByMentor(Long mentorId) {
        return milestoneRepository.findByMentorId(mentorId);
    }

    @Override
    public List<Milestone> getCompletedMilestones(Long startupId) {
        return milestoneRepository.findByStartupIdAndStatus(startupId, "COMPLETED");
    }

    @Override
    public List<Milestone> getPendingMilestones(Long startupId) {
        return milestoneRepository.findByStartupIdAndStatus(startupId, "PENDING");
    }

    @Override
    public Milestone updateMilestone(Milestone milestone) {
        milestone.setUpdatedAt(LocalDateTime.now());
        return milestoneRepository.save(milestone);
    }

    @Override
    public void deleteMilestone(Long id) {
        milestoneRepository.deleteById(id);
    }

    @Override
    public long countCompletedMilestones(Long startupId) {
        return milestoneRepository.countByStartupIdAndStatus(startupId, "COMPLETED");
    }

    @Override
    public long countPendingMilestones(Long startupId) {
        return milestoneRepository.countByStartupIdAndStatus(startupId, "PENDING");
    }

    @Override
    public long countByMentor(Long mentorId) {
        return milestoneRepository.findByMentorId(mentorId).size();
    }
}