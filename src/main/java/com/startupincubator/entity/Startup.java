package com.startupincubator.entity;

import com.startupincubator.enums.StartupStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "startups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StartupStatus status = StartupStatus.PENDING;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "team_size")
    @Builder.Default
    private Integer teamSize = 1;

    private String website;

    // ✅ ADD THIS - Mentor ID field
    @Column(name = "mentor_id")
    private Long mentorId;

    // ✅ ADD THIS - Mentor name field
    @Column(name = "mentor_name")
    private String mentorName;

    @Column(name = "founder_id")
    private Long founderId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private Double totalFundingAmount = 0.0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Getter and Setter for mentorId
    public Long getMentorId() {
        return mentorId;
    }

    public void setMentorId(Long mentorId) {
        this.mentorId = mentorId;
    }

    public String getMentorName() {
        return mentorName;
    }

    public void setMentorName(String mentorName) {
        this.mentorName = mentorName;
    }

    public Double getTotalFundingAmount() {
        return totalFundingAmount != null ? totalFundingAmount : 0.0;
    }

    public void setTotalFundingAmount(Double totalFundingAmount) {
        this.totalFundingAmount = totalFundingAmount;
    }
}