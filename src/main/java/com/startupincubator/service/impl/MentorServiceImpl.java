package com.startupincubator.service.impl;

import com.startupincubator.entity.Mentor;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MentorServiceImpl implements MentorService {

    private final MentorRepository mentorRepository;

    @Override
    public Mentor createMentor(Mentor mentor) {
        mentor.setCreatedAt(LocalDateTime.now());
        mentor.setUpdatedAt(LocalDateTime.now());
        return mentorRepository.save(mentor);
    }

    @Override
    public Optional<Mentor> findById(Long id) {
        return mentorRepository.findById(id);
    }

    @Override
    public Optional<Mentor> findByUserId(Long userId) {
        return mentorRepository.findByUserId(userId);
    }

    @Override
    public List<Mentor> findAllMentors() {
        return mentorRepository.findAll();
    }

    @Override
    public List<Mentor> findAvailableMentors() {
        return mentorRepository.findByAvailableTrue();
    }

    @Override
    public Mentor updateMentor(Mentor mentor) {
        mentor.setUpdatedAt(LocalDateTime.now());
        return mentorRepository.save(mentor);
    }

    @Override
    public void deleteMentor(Long id) {
        mentorRepository.deleteById(id);
    }

    // ✅ ADD THESE METHODS
    @Override
    public long countMentors() {
        return mentorRepository.count();
    }

    @Override
    public long countAvailableMentors() {
        return mentorRepository.countByAvailableTrue();
    }

    @Override
    public double getAverageRating() {
        Double avg = mentorRepository.getAverageRating();
        return avg != null ? avg : 0.0;
    }

    @Override
    public List<Mentor> findMentorsByExpertise(String expertise) {
        return mentorRepository.findByExpertiseContaining(expertise);
    }
}