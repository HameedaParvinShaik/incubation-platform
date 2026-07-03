package com.startupincubator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "startup_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role")
    private String role; // FOUNDER, CO-FOUNDER, TEAM_LEAD, DEVELOPER, etc.

    @Column(name = "is_active")
    private Boolean isActive = true;
}