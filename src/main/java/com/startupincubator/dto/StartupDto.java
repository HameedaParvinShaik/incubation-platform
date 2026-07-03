package com.startupincubator.dto;

import com.startupincubator.enums.StartupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private StartupStatus status;
    private Integer teamSize;
    private String website;
    private Long userId;
    private LocalDateTime createdAt;
    private Boolean isApproved;
}