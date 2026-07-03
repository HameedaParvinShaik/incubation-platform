package com.startupincubator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mentors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Mentor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String expertise;

    @Column(name = "experience_years")
    private Integer experienceYears;

    private String company;

    private String designation;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "is_available")
    @Builder.Default
    private Boolean available = true;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "current_startups")
    @Builder.Default
    private Integer currentStartups = 0;

    @Column(name = "max_startups")
    @Builder.Default
    private Integer maxStartups = 5;

    private Double rating;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    // ✅ ADD THESE FIELDS
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Relationship with reviews (if needed)
    @OneToMany(mappedBy = "mentor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MentorReview> reviews = new ArrayList<>();

    // ✅ Helper method to update rating
    public void updateRating() {
        if (reviews != null && !reviews.isEmpty()) {
            double avg = reviews.stream()
                    .mapToInt(MentorReview::getRating)
                    .average()
                    .orElse(0.0);
            this.rating = avg;
            this.totalReviews = reviews.size();
        } else {
            this.rating = 0.0;
            this.totalReviews = 0;
        }
    }
}