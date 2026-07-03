package com.startupincubator.service;

import com.startupincubator.entity.Mentor;
import java.util.List;
import java.util.Optional;

public interface MentorService {

    Mentor createMentor(Mentor mentor);
    Optional<Mentor> findById(Long id);
    Optional<Mentor> findByUserId(Long userId);
    List<Mentor> findAllMentors();
    List<Mentor> findAvailableMentors();
    Mentor updateMentor(Mentor mentor);
    void deleteMentor(Long id);
    
    // ✅ ADD THESE METHODS
    long countMentors();
    long countAvailableMentors();
    double getAverageRating();
    List<Mentor> findMentorsByExpertise(String expertise);
}