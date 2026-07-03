package com.startupincubator.controller;

import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.repository.MentorRepository;
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
@RequestMapping("/evaluator")
@RequiredArgsConstructor
@Slf4j
public class EvaluatorController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;

    // =============================================
    // EVALUATOR DASHBOARD
    // =============================================
    @GetMapping("/dashboard")
    public String evaluatorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long completedStartups = startupRepository.countByStatus(StartupStatus.COMPLETED);
        long totalMentors = mentorRepository.count();
        long totalUsers = userRepository.count();

        List<Startup> pendingList = startupRepository.findByStatus(StartupStatus.PENDING);

        model.addAttribute("userEmail", email);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("completedStartups", completedStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("pendingList", pendingList);

        return "dashboard/evaluator";
    }

    // =============================================
    // EVALUATIONS - View pending evaluations
    // =============================================
    @GetMapping("/evaluations")
    public String evaluations(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        List<Startup> pendingList = startupRepository.findByStatus(StartupStatus.PENDING);
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);

        model.addAttribute("userEmail", email);
        model.addAttribute("pendingList", pendingList);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);

        return "evaluator/evaluations";
    }

    // =============================================
    // EVALUATE STARTUP - Show evaluation form
    // =============================================
    @GetMapping("/evaluate/{id}")
    public String evaluateStartupForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));

        model.addAttribute("startup", startup);
        model.addAttribute("userEmail", email);

        return "evaluator/evaluate";
    }

    // =============================================
    // SUBMIT EVALUATION
    // =============================================
    @PostMapping("/evaluate/{id}")
    public String submitEvaluation(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String feedback,
            RedirectAttributes redirectAttributes) {
        
        try {
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            if ("APPROVE".equals(decision)) {
                startup.setStatus(StartupStatus.APPROVED);
                startup.setIsApproved(true);
                startup.setApprovedAt(LocalDateTime.now());
                startup.setApprovedBy(email);
                redirectAttributes.addFlashAttribute("success", "✅ Startup approved successfully!");
            } else if ("REJECT".equals(decision)) {
                startup.setStatus(StartupStatus.REJECTED);
                startup.setIsApproved(false);
                redirectAttributes.addFlashAttribute("success", "❌ Startup rejected!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid decision!");
                return "redirect:/evaluator/evaluate/" + id;
            }

            startup.setUpdatedAt(LocalDateTime.now());
            startupRepository.save(startup);

            log.info("📋 Startup {} {} by evaluator: {}", startup.getName(), decision, email);

        } catch (Exception e) {
            log.error("Error evaluating startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to evaluate startup: " + e.getMessage());
        }

        return "redirect:/evaluator/evaluations";
    }

    // =============================================
    // REPORTS
    // =============================================
    @GetMapping("/reports")
    public String reports(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long completedStartups = startupRepository.countByStatus(StartupStatus.COMPLETED);

        int approvalRate = totalStartups > 0 ? (int) ((approvedStartups * 100) / totalStartups) : 0;
        int pendingRate = totalStartups > 0 ? (int) ((pendingStartups * 100) / totalStartups) : 0;
        int rejectionRate = totalStartups > 0 ? (int) ((rejectedStartups * 100) / totalStartups) : 0;

        model.addAttribute("userEmail", email);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("completedStartups", completedStartups);
        model.addAttribute("approvalRate", approvalRate);
        model.addAttribute("pendingRate", pendingRate);
        model.addAttribute("rejectionRate", rejectionRate);

        return "evaluator/reports";
    }

    // =============================================
    // PROFILE
    // =============================================
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("userEmail", email);

        return "evaluator/profile";
    }

    // =============================================
    // EDIT PROFILE - Show form
    // =============================================
    @GetMapping("/profile/edit")
    public String editProfileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("userEmail", email);

        return "evaluator/profile-edit";
    }

    // =============================================
    // UPDATE PROFILE
    // =============================================
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phoneNumber,
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/auth/login";
            }

            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(phoneNumber);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/evaluator/profile";

        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/evaluator/profile/edit";
        }
    }
}