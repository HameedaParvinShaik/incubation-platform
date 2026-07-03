package com.startupincubator.service;

import com.startupincubator.entity.Milestone;

import java.util.List;
import java.util.Optional;

public interface MilestoneService {

    Milestone createMilestone(Milestone milestone);

    Optional<Milestone> findById(Long id);

    List<Milestone> getMilestonesByStartup(Long startupId);

    List<Milestone> getMilestonesByMentor(Long mentorId);

    List<Milestone> getCompletedMilestones(Long startupId);

    List<Milestone> getPendingMilestones(Long startupId);

    Milestone updateMilestone(Milestone milestone);

    void deleteMilestone(Long id);

    long countCompletedMilestones(Long startupId);

    long countPendingMilestones(Long startupId);

    long countByMentor(Long mentorId);
}