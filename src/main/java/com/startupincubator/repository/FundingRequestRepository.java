package com.startupincubator.repository;

import com.startupincubator.entity.FundingRequest;
import com.startupincubator.enums.FundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundingRequestRepository extends JpaRepository<FundingRequest, Long> {

    // Find by startup ID
    List<FundingRequest> findByStartupId(Long startupId);
    
    // Find by startup ID and status
    List<FundingRequest> findByStartupIdAndStatus(Long startupId, FundingStatus status);
    
    // ✅ ADD THIS - Find by user ID (investor)
    List<FundingRequest> findByUserId(Long userId);
    
    // Find by user ID and status
    List<FundingRequest> findByUserIdAndStatus(Long userId, FundingStatus status);
    
    // Alias for findByUserIdAndStatus (for clarity)
    default List<FundingRequest> findByInvestorIdAndStatus(Long investorId, FundingStatus status) {
        return findByUserIdAndStatus(investorId, status);
    }
    
    // Count by status
    long countByStatus(FundingStatus status);
    
    // Find by status
    List<FundingRequest> findByStatus(FundingStatus status);
    
    // Find by startup ID and status in list
    List<FundingRequest> findByStartupIdAndStatusIn(Long startupId, List<FundingStatus> statuses);
}