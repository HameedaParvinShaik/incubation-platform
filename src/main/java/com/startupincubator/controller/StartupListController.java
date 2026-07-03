package com.startupincubator.controller;

import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.*;
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
@RequestMapping("/startups")
@RequiredArgsConstructor
@Slf4j
public class StartupListController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final MilestoneRepository milestoneRepository;
    // Add other repositories as needed
    // private final StartupMemberRepository startupMemberRepository;
    // private final FundingRequestRepository fundingRequestRepository;

    @GetMapping
    public String listStartups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_MANAGER"));
        
        List<Startup> startups;
        if (isAdmin || isManager) {
            if (status != null && !status.isEmpty()) {
                try {
                    StartupStatus startupStatus = StartupStatus.valueOf(status.toUpperCase());
                    startups = startupRepository.findByStatus(startupStatus);
                } catch (IllegalArgumentException e) {
                    startups = startupRepository.findAll();
                }
            } else if (search != null && !search.isEmpty()) {
                startups = startupRepository.findByNameContainingIgnoreCase(search);
            } else {
                startups = startupRepository.findAll();
            }
        } else {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                startups = startupRepository.findByUserId(user.getId());
            } else {
                startups = List.of();
            }
        }
        
        addCommonAttributes(model);
        model.addAttribute("startups", startups);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("userEmail", email);
        
        return "startup/list";
    }

    @GetMapping("/{id}")
    public String viewStartup(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        boolean isFounder = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_FOUNDER"));
        
        if (isFounder) {
            return "redirect:/founder/startups/view/" + id;
        }
        
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
        
        addCommonAttributes(model);
        model.addAttribute("startup", startup);
        model.addAttribute("userEmail", email);
        
        return "startup/view";
    }

    @GetMapping("/create")
    public String createStartupForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        addCommonAttributes(model);
        model.addAttribute("startup", new Startup());
        model.addAttribute("userEmail", email);
        model.addAttribute("statuses", StartupStatus.values());
        
        return "startup/create";
    }

    @PostMapping("/create")
    public String createStartup(
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer teamSize,
            @RequestParam(required = false) String website,
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            boolean isFounder = auth.getAuthorities().stream()
                    .anyMatch(r -> r.getAuthority().equals("ROLE_FOUNDER"));
            
            User user = userRepository.findByEmail(email).orElse(null);
            
            Startup startup = new Startup();
            startup.setName(name);
            startup.setCategory(category);
            startup.setDescription(description);
            startup.setTeamSize(teamSize != null ? teamSize : 1);
            startup.setWebsite(website);
            startup.setStatus(StartupStatus.PENDING);
            startup.setIsApproved(false);
            startup.setCreatedAt(LocalDateTime.now());
            startup.setUpdatedAt(LocalDateTime.now());
            
            if (user != null) {
                startup.setUserId(user.getId());
            }
            
            startupRepository.save(startup);
            
            redirectAttributes.addFlashAttribute("success", "Startup created successfully! Waiting for approval.");
            log.info("✅ Startup created: {}", startup.getName());
            
            if (isFounder) {
                return "redirect:/founder/startups";
            }
            return "redirect:/startups";
            
        } catch (Exception e) {
            log.error("Error creating startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to create startup: " + e.getMessage());
            return "redirect:/startups/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editStartup(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        boolean isFounder = false;
        boolean isAdmin = false;
        String role = "USER";
        
        if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            role = auth.getAuthorities().iterator().next().getAuthority();
            isFounder = role.equals("ROLE_FOUNDER");
            isAdmin = role.equals("ROLE_ADMIN");
        }
        
        log.info("🔵 Edit startup - User: {}, Role: {}, isFounder: {}", email, role, isFounder);
        
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
        
        if (isFounder) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || !startup.getUserId().equals(user.getId())) {
                log.warn("⚠️ Founder {} tried to edit startup {} but doesn't own it", email, id);
                return "redirect:/founder/startups?error=You don't have permission to edit this startup";
            }
        }
        
        addCommonAttributes(model);
        model.addAttribute("startup", startup);
        model.addAttribute("userEmail", email);
        model.addAttribute("statuses", StartupStatus.values());
        model.addAttribute("userRole", role);
        model.addAttribute("isFounder", isFounder);
        model.addAttribute("isAdmin", isAdmin);
        
        return "startup/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateStartup(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            boolean isFounder = false;
            String role = "USER";
            if (auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
                role = auth.getAuthorities().iterator().next().getAuthority();
                isFounder = role.equals("ROLE_FOUNDER");
            }
            
            log.info("🔄 Updating startup {} by {} (Role: {})", id, email, role);
            
            Startup existing = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            if (isFounder) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null || !existing.getUserId().equals(user.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You don't have permission to edit this startup!");
                    return "redirect:/founder/startups";
                }
            }
            
            existing.setName(name);
            existing.setCategory(category);
            existing.setDescription(description);
            existing.setUpdatedAt(LocalDateTime.now());
            
            if (!isFounder && status != null && !status.isEmpty()) {
                try {
                    StartupStatus newStatus = StartupStatus.valueOf(status);
                    existing.setStatus(newStatus);
                    if (newStatus == StartupStatus.APPROVED) {
                        existing.setIsApproved(true);
                        existing.setApprovedAt(LocalDateTime.now());
                    } else if (newStatus == StartupStatus.REJECTED) {
                        existing.setIsApproved(false);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status: {}", status);
                }
            }
            
            startupRepository.save(existing);
            
            redirectAttributes.addFlashAttribute("success", "Startup updated successfully!");
            log.info("✅ Startup {} updated by {}", existing.getName(), email);
            
            if (isFounder) {
                return "redirect:/founder/startups";
            }
            return "redirect:/startups";
            
        } catch (Exception e) {
            log.error("Error updating startup: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update startup: " + e.getMessage());
            return "redirect:/startups";
        }
    }

    // =============================================
    // DELETE STARTUP - WITH RELATED DATA CLEANUP
    // =============================================
    @PostMapping("/{id}/delete")
    public String deleteStartup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            boolean isFounder = auth.getAuthorities().stream()
                    .anyMatch(r -> r.getAuthority().equals("ROLE_FOUNDER"));
            
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            if (isFounder) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null || !startup.getUserId().equals(user.getId())) {
                    redirectAttributes.addFlashAttribute("error", "You don't have permission to delete this startup!");
                    return "redirect:/founder/startups";
                }
            }
            
            // ✅ DELETE RELATED DATA FIRST
            
            // 1. Delete related milestones
            milestoneRepository.deleteByStartupId(id);
            log.info("🗑️ Deleted milestones for startup ID: {}", id);
            
            // 2. Delete related funding requests (if repository exists)
            // fundingRequestRepository.deleteByStartupId(id);
            
            // 3. Delete related team members (if repository exists)
            // startupMemberRepository.deleteByStartupId(id);
            
            // 4. Delete related reviews (if repository exists)
            // reviewRepository.deleteByStartupId(id);
            
            // Finally delete the startup
            startupRepository.delete(startup);
            
            redirectAttributes.addFlashAttribute("success", "Startup deleted successfully!");
            log.info("🗑️ Startup {} deleted", startup.getName());
            
            if (isFounder) {
                return "redirect:/founder/startups";
            }
            return "redirect:/startups";
            
        } catch (Exception e) {
            log.error("Error deleting startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete startup: " + e.getMessage());
            return "redirect:/startups";
        }
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
            log.info("✅ Startup {} approved", startup.getName());
            
        } catch (Exception e) {
            log.error("Error approving startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to approve startup: " + e.getMessage());
        }
        
        return "redirect:/startups";
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
            log.info("❌ Startup {} rejected", startup.getName());
            
        } catch (Exception e) {
            log.error("Error rejecting startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to reject startup: " + e.getMessage());
        }
        
        return "redirect:/startups";
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
            log.info("🟢 Startup {} activated", startup.getName());
            
        } catch (Exception e) {
            log.error("Error activating startup: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to activate startup: " + e.getMessage());
        }
        
        return "redirect:/startups";
    }

    private void addCommonAttributes(Model model) {
        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorRepository.count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
    }
}