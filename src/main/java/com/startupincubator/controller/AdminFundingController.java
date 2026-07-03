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
import java.util.List;

@Controller
@RequestMapping("/admin/funding")
@RequiredArgsConstructor
@Slf4j
public class AdminFundingController {

    private final FundingRequestRepository fundingRequestRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorService mentorService;

    // =============================================
    // ADMIN FUNDING LIST
    // =============================================
    @GetMapping
    public String listFunding(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        List<FundingRequest> allRequests = fundingRequestRepository.findAll();
        long totalRequests = fundingRequestRepository.count();
        long totalPending = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long totalApproved = fundingRequestRepository.countByStatus(FundingStatus.APPROVED);
        long totalFunded = fundingRequestRepository.countByStatus(FundingStatus.FUNDED);
        long totalRejected = fundingRequestRepository.countByStatus(FundingStatus.REJECTED);

        double totalFundingAmount = fundingRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();

        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequests", allRequests);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalApproved", totalApproved);
        model.addAttribute("totalFunded", totalFunded);
        model.addAttribute("totalRejected", totalRejected);
        model.addAttribute("totalFundingAmount", totalFundingAmount);

        return "admin/funding/list";
    }

    // =============================================
    // ADMIN VIEW FUNDING REQUEST
    // =============================================
    @GetMapping("/{id}")
    public String viewFundingRequest(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funding request not found"));

        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequest", fundingRequest);

        return "admin/funding/view";
    }

    // =============================================
    // ADMIN APPROVE FUNDING
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

            fundingRequestRepository.save(fundingRequest);

            redirectAttributes.addFlashAttribute("success", "Funding request approved successfully!");

        } catch (Exception e) {
            log.error("Error approving funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to approve funding request: " + e.getMessage());
        }

        return "redirect:/admin/funding";
    }

    // =============================================
    // ADMIN REJECT FUNDING
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

        return "redirect:/admin/funding";
    }

    // =============================================
    // ADMIN MARK AS FUNDED
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

        return "redirect:/admin/funding";
    }

    // =============================================
    // ADMIN DELETE FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/delete")
    public String deleteFundingRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));

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

        return "redirect:/admin/funding";
    }
}