package com.startupincubator.controller;

import com.startupincubator.entity.FundingRequest;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.FundingStatus;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.FundingRequestRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.service.MentorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/funding")
@RequiredArgsConstructor
@Slf4j
public class FundingController {

    private final FundingRequestRepository fundingRequestRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorService mentorService;

    // =============================================
    // FUNDING DASHBOARD - uses funding/index.html
    // =============================================
    @GetMapping
    public String fundingDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        // Get all funding requests
        List<FundingRequest> allRequests = fundingRequestRepository.findAll();
        List<FundingRequest> pendingRequests = fundingRequestRepository.findByStatus(FundingStatus.PENDING);
        List<FundingRequest> approvedRequests = fundingRequestRepository.findByStatus(FundingStatus.APPROVED);
        List<FundingRequest> fundedRequests = fundingRequestRepository.findByStatus(FundingStatus.FUNDED);
        List<FundingRequest> rejectedRequests = fundingRequestRepository.findByStatus(FundingStatus.REJECTED);
        
        // Statistics
        long totalRequests = fundingRequestRepository.count();
        long totalPending = pendingRequests.size();
        long totalApproved = approvedRequests.size();
        long totalFunded = fundedRequests.size();
        long totalRejected = rejectedRequests.size();
        
        // Total funding amount
        double totalFundingAmount = fundingRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();
        
        // Add common attributes
        addCommonAttributes(model);
        
        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequests", allRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("fundedRequests", fundedRequests);
        model.addAttribute("rejectedRequests", rejectedRequests);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalApproved", totalApproved);
        model.addAttribute("totalFunded", totalFunded);
        model.addAttribute("totalRejected", totalRejected);
        model.addAttribute("totalFundingAmount", totalFundingAmount);
        
        return "funding/index";
    }

    // =============================================
    // VIEW ALL FUNDING REQUESTS
    // =============================================
    @GetMapping("/requests")
    public String fundingRequests(
            @RequestParam(required = false) String status,
            Model model) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        List<FundingRequest> requests;
        if (status != null && !status.isEmpty()) {
            try {
                FundingStatus fundingStatus = FundingStatus.valueOf(status.toUpperCase());
                requests = fundingRequestRepository.findByStatus(fundingStatus);
            } catch (IllegalArgumentException e) {
                requests = fundingRequestRepository.findAll();
            }
        } else {
            requests = fundingRequestRepository.findAll();
        }
        
        // Calculate stats
        long totalRequests = fundingRequestRepository.count();
        long totalPending = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long totalFunded = fundingRequestRepository.countByStatus(FundingStatus.FUNDED);
        double totalFundingAmount = fundingRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();
        
        addCommonAttributes(model);
        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequests", requests);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalFunded", totalFunded);
        model.addAttribute("totalFundingAmount", totalFundingAmount);
        
        return "funding/requests";
    }

    // =============================================
    // CREATE FUNDING REQUEST - SHOW FORM
    // =============================================
    @GetMapping("/create")
    public String createFundingRequestForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        log.info("📍 User {} accessing funding creation form", email);
        log.info("👤 User roles: {}", user.getRoles());
        
        // Get user's startups (for founder)
        List<Startup> userStartups = startupRepository.findByUserId(user.getId());
        log.info("📋 User startups count: {}", userStartups.size());
        
        // Get all approved startups (for admin/manager)
        List<Startup> approvedStartups = startupRepository.findByStatus(StartupStatus.APPROVED);
        log.info("📋 Approved startups count: {}", approvedStartups.size());
        
        // If no approved startups, show all startups
        if (approvedStartups.isEmpty()) {
            approvedStartups = startupRepository.findAll();
            log.info("📋 All startups count (fallback): {}", approvedStartups.size());
        }
        
        addCommonAttributes(model);
        model.addAttribute("userEmail", email);
        model.addAttribute("userStartups", userStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("fundingRequest", new FundingRequest());
        
        return "funding/create";
    }

    // =============================================
    // CREATE FUNDING REQUEST - SUBMIT
    // =============================================
    @PostMapping("/create")
public String createFundingRequest(
        @RequestParam Long startupId,
        @RequestParam Double amount,
        @RequestParam String purpose,
        @RequestParam(required = false) String description,
        RedirectAttributes redirectAttributes) {
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    
    try {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found!");
            return "redirect:/auth/login";
        }
        
        Startup startup = startupRepository.findById(startupId)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
        
        log.info("✅ User {} creating funding request for startup: {}", email, startup.getName());
        
        FundingRequest fundingRequest = new FundingRequest();
        fundingRequest.setStartup(startup);
        fundingRequest.setStartupId(startupId);
        fundingRequest.setUserId(user.getId());  // ✅ IMPORTANT: Set the user_id
        fundingRequest.setAmount(amount);
        fundingRequest.setPurpose(purpose);
        fundingRequest.setDescription(description);
        fundingRequest.setStatus(FundingStatus.PENDING);
        fundingRequest.setRequestedBy(user.getFirstName() + " " + user.getLastName());
        fundingRequest.setRequestedAt(LocalDateTime.now());
        fundingRequest.setCreatedAt(LocalDateTime.now());
        fundingRequest.setUpdatedAt(LocalDateTime.now());
        
        fundingRequestRepository.save(fundingRequest);
        
        redirectAttributes.addFlashAttribute("success", "Funding request created successfully!");
        return "redirect:/funding";
        
    } catch (Exception e) {
        log.error("Error creating funding request: {}", e.getMessage());
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Failed to create funding request: " + e.getMessage());
        return "redirect:/funding/create";
    }
}

    // =============================================
    // VIEW FUNDING REQUEST DETAILS
    // =============================================
    @GetMapping("/{id}")
    public String viewFundingRequest(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funding request not found"));
        
        addCommonAttributes(model);
        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequest", fundingRequest);
        
        return "funding/view";
    }

    // =============================================
    // APPROVE FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/approve")
    public String approveFundingRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            fundingRequest.setStatus(FundingStatus.APPROVED);
            fundingRequest.setApprovedBy(email);
            fundingRequest.setApprovedAt(LocalDateTime.now());
            fundingRequest.setUpdatedAt(LocalDateTime.now());
            if (notes != null && !notes.isEmpty()) {
                fundingRequest.setNotes(notes);
            }
            
            fundingRequestRepository.save(fundingRequest);
            
            redirectAttributes.addFlashAttribute("success", "Funding request approved successfully!");
            
        } catch (Exception e) {
            log.error("Error approving funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to approve funding request: " + e.getMessage());
        }
        
        return "redirect:/funding";
    }

    // =============================================
    // REJECT FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/reject")
    public String rejectFundingRequest(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));
            
            fundingRequest.setStatus(FundingStatus.REJECTED);
            fundingRequest.setRejectionReason(reason);
            fundingRequest.setUpdatedAt(LocalDateTime.now());
            
            fundingRequestRepository.save(fundingRequest);
            
            redirectAttributes.addFlashAttribute("success", "Funding request rejected.");
            
        } catch (Exception e) {
            log.error("Error rejecting funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to reject funding request: " + e.getMessage());
        }
        
        return "redirect:/funding";
    }

    // =============================================
    // MARK AS FUNDED
    // =============================================
    @PostMapping("/{id}/fund")
    public String markAsFunded(
            @PathVariable Long id,
            @RequestParam(required = false) String transactionId,
            RedirectAttributes redirectAttributes) {
        
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));
            
            fundingRequest.setStatus(FundingStatus.FUNDED);
            fundingRequest.setTransactionId(transactionId);
            fundingRequest.setFundedAt(LocalDateTime.now());
            fundingRequest.setUpdatedAt(LocalDateTime.now());
            
            fundingRequestRepository.save(fundingRequest);
            
            redirectAttributes.addFlashAttribute("success", "Funding marked as completed!");
            
        } catch (Exception e) {
            log.error("Error marking funding as completed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to mark funding as completed: " + e.getMessage());
        }
        
        return "redirect:/funding";
    }

    // =============================================
    // DELETE FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/delete")
    public String deleteFundingRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));
            
            // Only allow deletion if status is PENDING or REJECTED
            if (fundingRequest.getStatus() == FundingStatus.PENDING || 
                fundingRequest.getStatus() == FundingStatus.REJECTED) {
                fundingRequestRepository.delete(fundingRequest);
                redirectAttributes.addFlashAttribute("success", "Funding request deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Cannot delete approved or funded requests!");
            }
            
        } catch (Exception e) {
            log.error("Error deleting funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete funding request: " + e.getMessage());
        }
        
        return "redirect:/funding";
    }

    // =============================================
    // FUNDING STATISTICS API (for charts)
    // =============================================
    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getFundingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalRequests = fundingRequestRepository.count();
        long pending = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long approved = fundingRequestRepository.countByStatus(FundingStatus.APPROVED);
        long funded = fundingRequestRepository.countByStatus(FundingStatus.FUNDED);
        long rejected = fundingRequestRepository.countByStatus(FundingStatus.REJECTED);
        
        double totalAmount = fundingRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();
        
        stats.put("totalRequests", totalRequests);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("funded", funded);
        stats.put("rejected", rejected);
        stats.put("totalAmount", totalAmount);
        
        // Monthly stats for chart
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        Map<String, Integer> monthMap = new LinkedHashMap<>();
        
        fundingRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == FundingStatus.FUNDED)
                .forEach(r -> {
                    String month = r.getFundedAt() != null ? 
                        r.getFundedAt().getMonth().toString() : "Unknown";
                    monthMap.put(month, monthMap.getOrDefault(month, 0) + 1);
                });
        
        for (Map.Entry<String, Integer> entry : monthMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("count", entry.getValue());
            monthlyData.add(item);
        }
        
        stats.put("monthlyData", monthlyData);
        
        return stats;
    }

    // =============================================
    // HELPER METHODS
    // =============================================
    private void addCommonAttributes(Model model) {
        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorService.countMentors();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
    }
}