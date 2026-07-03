package com.startupincubator.repository;

import com.startupincubator.entity.MentorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorReviewRepository extends JpaRepository<MentorReview, Long> {
    List<MentorReview> findByMentorId(Long mentorId);
    List<MentorReview> findByReviewerId(Long reviewerId);
    boolean existsByMentorIdAndReviewerId(Long mentorId, Long reviewerId);
    long countByMentorId(Long mentorId);
}