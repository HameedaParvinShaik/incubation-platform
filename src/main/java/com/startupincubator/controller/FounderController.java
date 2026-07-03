package com.startupincubator.controller;

import com.startupincubator.entity.Milestone;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.TeamMember;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.MilestoneRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.TeamMemberRepository;
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/founder")
@RequiredArgsConstructor
@Slf4j
public class FounderController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final TeamMemberRepository teamMemberRepository;

    // =============================================
    // LIST FOUNDER'S STARTUPS
    // =============================================
    @GetMapping("/startups")
    public String myStartups(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<Startup> myStartups = startupRepository.findByUserId(user.getId());
        long myStartupsCount = myStartups.size();
        long approvedCount = myStartups.stream()
                .filter(s -> s.getStatus() == StartupStatus.APPROVED)
                .count();
        long pendingCount = myStartups.stream()
                .filter(s -> s.getStatus() == StartupStatus.PENDING)
                .count();
        
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("myStartupsCount", myStartupsCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        
        return "founder/startups";
    }

    // =============================================
    // VIEW FOUNDER'S STARTUP DETAILS
    // =============================================
    @GetMapping("/startups/view/{id}")
    public String viewStartup(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
        
        if (!startup.getUserId().equals(user.getId())) {
            return "redirect:/founder/startups";
        }
        
        model.addAttribute("startup", startup);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        
        return "founder/startup-view";
    }

    // =============================================
    // PROFILE
    // =============================================
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        
        return "founder/profile";
    }

    // =============================================
    // EDIT PROFILE - Show Edit Form
    // =============================================
    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        
        return "founder/profile-edit";
    }

    // =============================================
    // UPDATE PROFILE - Save Changes
    // =============================================
    @PostMapping("/profile/edit")
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
                return "redirect:/auth/login";
            }
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                user.setPhoneNumber(phoneNumber);
            }
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/founder/profile";
            
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/founder/profile/edit";
        }
    }

    // =============================================
    // TEAM - VIEW TEAM MEMBERS
    // =============================================
    @GetMapping("/team")
    public String team(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        String userName = user.getFirstName() + " " + user.getLastName();
        List<TeamMember> teamMembers = new ArrayList<>();
        long teamMembersCount = 0;

        try {
            List<Startup> startups = startupRepository.findByUserId(user.getId());
            if (!startups.isEmpty()) {
                Startup startup = startups.get(0);
                teamMembers = teamMemberRepository.findByStartupId(startup.getId());
                teamMembersCount = teamMembers.size();
                log.info("📋 Found {} team members for startup: {}", teamMembersCount, startup.getName());
            }
        } catch (Exception e) {
            log.error("Error fetching team members: {}", e.getMessage());
        }
        
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", email);
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("teamMembersCount", teamMembersCount);
        
        return "founder/team";
    }

    // =============================================
    // ADD TEAM MEMBER
    // =============================================
    @PostMapping("/team/add")
    public String addTeamMember(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Startup> startups = startupRepository.findByUserId(user.getId());
            if (startups.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ Please create a startup first!");
                return "redirect:/founder/team";
            }
            Startup startup = startups.get(0);

            List<TeamMember> existingMembers = teamMemberRepository.findByStartupId(startup.getId());
            boolean exists = existingMembers.stream().anyMatch(m -> m.getEmail().equals(email));
            
            if (exists) {
                redirectAttributes.addFlashAttribute("error", "❌ Team member with email '" + email + "' already exists!");
                return "redirect:/founder/team";
            }

            TeamMember teamMember = TeamMember.builder()
                    .name(name)
                    .email(email)
                    .role(role)
                    .status("ACTIVE")
                    .isActive(true)
                    .startup(startup)
                    .build();

            teamMemberRepository.save(teamMember);

            log.info("✅ Team member added: {} - {} to startup: {}", name, role, startup.getName());
            redirectAttributes.addFlashAttribute("success", "✅ Team member '" + name + "' added successfully!");

        } catch (Exception e) {
            log.error("Error adding team member: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to add team member: " + e.getMessage());
        }

        return "redirect:/founder/team";
    }

    // =============================================
    // REMOVE TEAM MEMBER
    // =============================================
    @PostMapping("/team/{id}/remove")
    public String removeTeamMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();

            TeamMember teamMember = teamMemberRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Team member not found"));

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Startup> startups = startupRepository.findByUserId(user.getId());
            if (startups.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ No startup found!");
                return "redirect:/founder/team";
            }
            Startup startup = startups.get(0);

            if (!teamMember.getStartup().getId().equals(startup.getId())) {
                redirectAttributes.addFlashAttribute("error", "❌ You don't have permission to remove this team member!");
                return "redirect:/founder/team";
            }

            String memberName = teamMember.getName();
            teamMemberRepository.deleteById(id);

            log.info("🗑️ Team member removed: {}", memberName);
            redirectAttributes.addFlashAttribute("success", "🗑️ Team member '" + memberName + "' removed successfully!");

        } catch (Exception e) {
            log.error("Error removing team member: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to remove team member: " + e.getMessage());
        }

        return "redirect:/founder/team";
    }

    // =============================================
    // UPDATE TEAM MEMBER STATUS
    // =============================================
    @PostMapping("/team/{id}/status")
    public String updateTeamMemberStatus(@PathVariable Long id,
                                         @RequestParam String status,
                                         RedirectAttributes redirectAttributes) {
        try {
            TeamMember teamMember = teamMemberRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Team member not found"));

            teamMember.setStatus(status);
            teamMemberRepository.save(teamMember);

            log.info("✅ Team member status updated: {} -> {}", teamMember.getName(), status);
            redirectAttributes.addFlashAttribute("success", "✅ Team member status updated!");

        } catch (Exception e) {
            log.error("Error updating team member status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to update status: " + e.getMessage());
        }

        return "redirect:/founder/team";
    }

    // =============================================
    // MILESTONES
    // =============================================
    @GetMapping("/milestones")
    public String milestones(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<Startup> myStartups = startupRepository.findByUserId(user.getId());
        
        List<Milestone> allMilestones = new ArrayList<>();
        for (Startup startup : myStartups) {
            List<Milestone> startupMilestones = milestoneRepository.findByStartupId(startup.getId());
            allMilestones.addAll(startupMilestones);
        }
        
        long completedCount = allMilestones.stream()
                .filter(m -> "COMPLETED".equals(m.getStatus()))
                .count();
        long pendingCount = allMilestones.stream()
                .filter(m -> "PENDING".equals(m.getStatus()) || m.getStatus() == null)
                .count();
        
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("milestones", allMilestones);
        model.addAttribute("totalCount", allMilestones.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        
        return "founder/milestones";
    }

    // =============================================
    // SHOW ADD MILESTONE FORM
    // =============================================
    @GetMapping("/milestones/add")
    public String addMilestoneForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<Startup> myStartups = startupRepository.findByUserId(user.getId());
        
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("userEmail", email);
        model.addAttribute("startups", myStartups);
        model.addAttribute("milestone", new Milestone());
        
        return "founder/milestone-add";
    }

    // =============================================
    // SAVE MILESTONE
    // =============================================
    @PostMapping("/milestones/add")
    public String saveMilestone(
            @RequestParam Long startupId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDate,
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            if (!startup.getUserId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to add milestones to this startup!");
                return "redirect:/founder/milestones";
            }
            
            Milestone milestone = new Milestone();
            milestone.setStartupId(startupId);
            milestone.setTitle(title);
            milestone.setDescription(description);
            milestone.setStatus("PENDING");
            milestone.setDueDate(dueDate);
            milestone.setProgressPercentage(0);
            milestone.setCreatedAt(LocalDateTime.now());
            milestone.setUpdatedAt(LocalDateTime.now());
            
            milestoneRepository.save(milestone);
            log.info("✅ Milestone saved: {}", milestone.getTitle());
            
            redirectAttributes.addFlashAttribute("success", "✅ Milestone added successfully!");
            
        } catch (Exception e) {
            log.error("❌ Error adding milestone: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to add milestone: " + e.getMessage());
        }
        
        return "redirect:/founder/milestones";
    }

    // =============================================
    // DELETE MILESTONE
    // =============================================
    @GetMapping("/milestones/delete/{id}")
    public String deleteMilestone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Milestone milestone = milestoneRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            milestoneRepository.delete(milestone);
            redirectAttributes.addFlashAttribute("success", "✅ Milestone deleted successfully!");
            
        } catch (Exception e) {
            log.error("❌ Error deleting milestone: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete milestone: " + e.getMessage());
        }
        return "redirect:/founder/milestones";
    }

    // =============================================
    // COMPLETE MILESTONE
    // =============================================
    @GetMapping("/milestones/complete/{id}")
    public String completeMilestone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Milestone milestone = milestoneRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            Startup startup = startupRepository.findById(milestone.getStartupId())
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            if (!startup.getUserId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission!");
                return "redirect:/founder/milestones";
            }
            
            milestone.setStatus("COMPLETED");
            milestone.setProgressPercentage(100);
            milestone.setCompletedAt(LocalDateTime.now());
            milestone.setUpdatedAt(LocalDateTime.now());
            
            milestoneRepository.save(milestone);
            log.info("✅ Milestone completed: {}", milestone.getTitle());
            
            redirectAttributes.addFlashAttribute("success", "✅ Milestone marked as completed!");
            
        } catch (Exception e) {
            log.error("❌ Error completing milestone: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to complete milestone: " + e.getMessage());
        }
        return "redirect:/founder/milestones";
    }
}