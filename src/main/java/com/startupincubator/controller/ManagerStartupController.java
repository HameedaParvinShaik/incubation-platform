package com.startupincubator.controller;

import com.startupincubator.entity.Mentor;
import com.startupincubator.entity.Startup;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.MentorRepository;
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
@RequestMapping("/manager/startups")
@RequiredArgsConstructor
@Slf4j
public class ManagerStartupController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;

    @GetMapping
    public String listStartups(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        List<Startup> startups = startupRepository.findAll();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long totalMentors = mentorRepository.count();
        long totalUsers = userRepository.count();

        model.addAttribute("userEmail", email);
        model.addAttribute("startups", startups);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("totalUsers", totalUsers);

        return "manager/startups/list";
    }

    @GetMapping("/{id}")
    public String viewStartup(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));

        model.addAttribute("userEmail", email);
        model.addAttribute("startup", startup);

        return "manager/startups/view";
    }

    @GetMapping("/{id}/edit")
    public String editStartupForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));

        List<Mentor> availableMentors = mentorRepository.findAvailableMentors();

        model.addAttribute("userEmail", email);
        model.addAttribute("startup", startup);
        model.addAttribute("statuses", StartupStatus.values());
        model.addAttribute("availableMentors", availableMentors);

        return "manager/startups/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateStartup(@PathVariable Long id,
                                @ModelAttribute Startup updatedStartup,
                                RedirectAttributes redirectAttributes) {
        try {
            Startup existing = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            existing.setName(updatedStartup.getName());
            existing.setCategory(updatedStartup.getCategory());
            existing.setDescription(updatedStartup.getDescription());
            existing.setTeamSize(updatedStartup.getTeamSize());
            existing.setWebsite(updatedStartup.getWebsite());
            existing.setStatus(updatedStartup.getStatus());
            existing.setUpdatedAt(LocalDateTime.now());

            startupRepository.save(existing);

            redirectAttributes.addFlashAttribute("success", "✅ Startup updated successfully!");

        } catch (Exception e) {
            log.error("Error updating startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to update startup: " + e.getMessage());
        }
        return "redirect:/manager/startups";
    }

    // ✅ ASSIGN MENTOR TO STARTUP - FIXED
    @PostMapping("/{id}/assign-mentor")
    public String assignMentor(@PathVariable Long id,
                               @RequestParam Long mentorId,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            Mentor mentor = mentorRepository.findById(mentorId)
                    .orElseThrow(() -> new RuntimeException("Mentor not found"));

            startup.setMentorId(mentorId);
            startup.setMentorName(mentor.getName());

            // ✅ Only update status if it's APPROVED, don't touch others
            if (startup.getStatus() == StartupStatus.APPROVED) {
                startup.setStatus(StartupStatus.IN_PROGRESS);
            }
            // If status is ACTIVE, keep it ACTIVE
            // If status is PENDING, keep it PENDING

            startup.setUpdatedAt(LocalDateTime.now());
            startupRepository.save(startup);

            // Update mentor's current startups count
            mentor.setCurrentStartups(mentor.getCurrentStartups() != null ? mentor.getCurrentStartups() + 1 : 1);
            mentorRepository.save(mentor);

            log.info("✅ Mentor '{}' assigned to startup '{}'", mentor.getName(), startup.getName());

            redirectAttributes.addFlashAttribute("success", 
                    "✅ Mentor '" + mentor.getName() + "' assigned successfully!");

        } catch (Exception e) {
            log.error("Error assigning mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                    "❌ Failed to assign mentor: " + e.getMessage());
        }

        return "redirect:/manager/startups";
    }

    // ✅ REMOVE MENTOR FROM STARTUP - FIXED
    @PostMapping("/{id}/remove-mentor")
    public String removeMentor(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            if (startup.getMentorId() != null) {
                mentorRepository.findById(startup.getMentorId()).ifPresent(mentor -> {
                    mentor.setCurrentStartups(Math.max(0, mentor.getCurrentStartups() - 1));
                    mentorRepository.save(mentor);
                });
            }

            startup.setMentorId(null);
            startup.setMentorName(null);
            startup.setUpdatedAt(LocalDateTime.now());
            startupRepository.save(startup);

            log.info("🗑️ Mentor removed from startup '{}'", startup.getName());

            redirectAttributes.addFlashAttribute("success", 
                    "🗑️ Mentor removed successfully!");

        } catch (Exception e) {
            log.error("Error removing mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                    "❌ Failed to remove mentor: " + e.getMessage());
        }

        return "redirect:/manager/startups";
    }

    @PostMapping("/{id}/approve")
    public String approveStartup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            startup.setStatus(StartupStatus.APPROVED);
            startup.setIsApproved(true);
            startup.setApprovedAt(LocalDateTime.now());
            startup.setUpdatedAt(LocalDateTime.now());

            startupRepository.save(startup);

            redirectAttributes.addFlashAttribute("success", "Startup approved successfully!");

        } catch (Exception e) {
            log.error("Error approving startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to approve startup: " + e.getMessage());
        }

        return "redirect:/manager/startups";
    }

    @PostMapping("/{id}/reject")
    public String rejectStartup(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            startup.setStatus(StartupStatus.REJECTED);
            startup.setIsApproved(false);
            startup.setUpdatedAt(LocalDateTime.now());

            startupRepository.save(startup);

            redirectAttributes.addFlashAttribute("success", "Startup rejected!");

        } catch (Exception e) {
            log.error("Error rejecting startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to reject startup: " + e.getMessage());
        }

        return "redirect:/manager/startups";
    }

    @PostMapping("/{id}/activate")
    public String activateStartup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));

            startup.setStatus(StartupStatus.ACTIVE);
            startup.setUpdatedAt(LocalDateTime.now());

            startupRepository.save(startup);

            redirectAttributes.addFlashAttribute("success", "Startup activated!");

        } catch (Exception e) {
            log.error("Error activating startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to activate startup: " + e.getMessage());
        }

        return "redirect:/manager/startups";
    }
}