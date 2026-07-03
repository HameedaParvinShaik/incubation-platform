package com.startupincubator.controller;

import com.startupincubator.entity.FundingRequest;
import com.startupincubator.enums.FundingStatus;
import com.startupincubator.repository.FundingRequestRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
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
@RequestMapping("/manager/funding")
@RequiredArgsConstructor
@Slf4j
public class ManagerFundingController {

    private final FundingRequestRepository fundingRequestRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;

    // =============================================
    // MANAGER - VIEW ALL FUNDING REQUESTS
    // =============================================
    @GetMapping
    public String listFundingRequests(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        List<FundingRequest> allRequests = fundingRequestRepository.findAll();
        
        long totalRequests = fundingRequestRepository.count();
        long pendingCount = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long approvedCount = fundingRequestRepository.countByStatus(FundingStatus.APPROVED);
        long fundedCount = fundingRequestRepository.countByStatus(FundingStatus.FUNDED);
        long rejectedCount = fundingRequestRepository.countByStatus(FundingStatus.REJECTED);

        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequests", allRequests);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("fundedCount", fundedCount);
        model.addAttribute("rejectedCount", rejectedCount);

        return "manager/funding/list";
    }

    // =============================================
    // MANAGER - VIEW SINGLE FUNDING REQUEST
    // =============================================
    @GetMapping("/{id}")
    public String viewFundingRequest(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funding request not found"));

        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequest", fundingRequest);

        return "manager/funding/view";
    }

    // =============================================
    // MANAGER - APPROVE FUNDING REQUEST
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

            if (fundingRequest.getStatus() != FundingStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "This request has already been processed!");
                return "redirect:/manager/funding";
            }

            fundingRequest.setStatus(FundingStatus.APPROVED);
            fundingRequest.setApprovedBy(email);
            fundingRequest.setApprovedAt(LocalDateTime.now());
            fundingRequest.setNotes(notes);
            fundingRequest.setUpdatedAt(LocalDateTime.now());

            fundingRequestRepository.save(fundingRequest);

            log.info("✅ Manager {} approved funding request {}", email, id);
            redirectAttributes.addFlashAttribute("success", "✅ Funding request approved successfully!");

        } catch (Exception e) {
            log.error("Error approving funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to approve funding request: " + e.getMessage());
        }

        return "redirect:/manager/funding";
    }

    // =============================================
    // MANAGER - REJECT FUNDING REQUEST (FIXED)
    // =============================================
    @PostMapping("/{id}/reject")
    public String rejectFundingRequest(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();  // ✅ FIXED: Added this line

            if (fundingRequest.getStatus() != FundingStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "This request has already been processed!");
                return "redirect:/manager/funding";
            }

            fundingRequest.setStatus(FundingStatus.REJECTED);
            fundingRequest.setRejectionReason(reason);
            fundingRequest.setUpdatedAt(LocalDateTime.now());

            fundingRequestRepository.save(fundingRequest);

            log.info("❌ Manager {} rejected funding request {}", email, id);
            redirectAttributes.addFlashAttribute("success", "❌ Funding request rejected.");

        } catch (Exception e) {
            log.error("Error rejecting funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to reject funding request: " + e.getMessage());
        }

        return "redirect:/manager/funding";
    }

    // =============================================
    // MANAGER - MARK AS FUNDED
    // =============================================
    @PostMapping("/{id}/fund")
    public String markAsFunded(
            @PathVariable Long id,
            @RequestParam(required = false) String transactionId,
            RedirectAttributes redirectAttributes) {

        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            if (fundingRequest.getStatus() != FundingStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("error", "Only approved requests can be marked as funded!");
                return "redirect:/manager/funding";
            }

            fundingRequest.setStatus(FundingStatus.FUNDED);
            fundingRequest.setTransactionId(transactionId != null ? transactionId : "TXN-" + System.currentTimeMillis());
            fundingRequest.setFundedAt(LocalDateTime.now());
            fundingRequest.setUpdatedAt(LocalDateTime.now());

            fundingRequestRepository.save(fundingRequest);

            log.info("💰 Manager {} marked funding request {} as funded", email, id);
            redirectAttributes.addFlashAttribute("success", "💰 Funding disbursed successfully!");

        } catch (Exception e) {
            log.error("Error marking funding as completed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to mark funding as completed: " + e.getMessage());
        }

        return "redirect:/manager/funding";
    }

    // =============================================
    // MANAGER - EDIT FUNDING REQUEST FORM
    // =============================================
    @GetMapping("/{id}/edit")
    public String editFundingRequestForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funding request not found"));

        model.addAttribute("userEmail", email);
        model.addAttribute("fundingRequest", fundingRequest);

        return "manager/funding/edit";
    }

    // =============================================
    // MANAGER - UPDATE FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/edit")
    public String updateFundingRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String purpose,
            RedirectAttributes redirectAttributes) {

        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            if (fundingRequest.getStatus() == FundingStatus.FUNDED || 
                fundingRequest.getStatus() == FundingStatus.REJECTED) {
                redirectAttributes.addFlashAttribute("error", "Cannot edit a funded or rejected request!");
                return "redirect:/manager/funding";
            }

            if (notes != null && !notes.isEmpty()) {
                fundingRequest.setNotes(notes);
            }
            if (purpose != null && !purpose.isEmpty()) {
                fundingRequest.setPurpose(purpose);
            }
            fundingRequest.setUpdatedAt(LocalDateTime.now());

            fundingRequestRepository.save(fundingRequest);

            log.info("✏️ Manager {} updated funding request {}", email, id);
            redirectAttributes.addFlashAttribute("success", "✅ Funding request updated successfully!");

        } catch (Exception e) {
            log.error("Error updating funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to update funding request: " + e.getMessage());
        }

        return "redirect:/manager/funding";
    }

    // =============================================
    // MANAGER - DELETE FUNDING REQUEST
    // =============================================
    @PostMapping("/{id}/delete")
    public String deleteFundingRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            FundingRequest fundingRequest = fundingRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Funding request not found"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            if (fundingRequest.getStatus() == FundingStatus.APPROVED || 
                fundingRequest.getStatus() == FundingStatus.FUNDED) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete an approved or funded request!");
                return "redirect:/manager/funding";
            }

            fundingRequestRepository.deleteById(id);

            log.info("🗑️ Manager {} deleted funding request {}", email, id);
            redirectAttributes.addFlashAttribute("success", "🗑️ Funding request deleted successfully!");

        } catch (Exception e) {
            log.error("Error deleting funding request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to delete funding request: " + e.getMessage());
        }

        return "redirect:/manager/funding";
    }
}