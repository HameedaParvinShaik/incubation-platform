package com.startupincubator.controller;

import com.startupincubator.entity.Milestone;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.repository.MilestoneRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final UserRepository userRepository;
    private final StartupRepository startupRepository;
    private final MilestoneRepository milestoneRepository;

    // =============================================
    // MAIN MILESTONES PAGE
    // =============================================
    @GetMapping
    public String milestonesPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User currentUser = userRepository.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        List<Startup> myStartups = startupRepository.findByUserId(currentUser.getId());
        
        model.addAttribute("userEmail", email);
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("message", "Select a startup to manage milestones");
        
        return "milestones/list";
    }

    // =============================================
    // VIEW MILESTONES FOR A SPECIFIC STARTUP
    // =============================================
    @GetMapping("/{startupId}")
    public String viewMilestones(@PathVariable Long startupId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User currentUser = userRepository.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        Startup startup = startupRepository.findById(startupId).orElse(null);
        if (startup == null) {
            return "redirect:/milestones";
        }

        List<Milestone> milestones = milestoneService.getMilestonesByStartup(startupId);
        long completedCount = milestoneService.countCompletedMilestones(startupId);
        long pendingCount = milestoneService.countPendingMilestones(startupId);
        
        List<Startup> myStartups = startupRepository.findByUserId(currentUser.getId());
        
        model.addAttribute("userEmail", email);
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("selectedStartup", startup);
        model.addAttribute("milestones", milestones);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("totalCount", milestones.size());
        
        return "milestones/view";
    }

    // =============================================
    // CREATE MILESTONE FORM
    // =============================================
    @GetMapping("/create")
    public String createMilestoneForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User currentUser = userRepository.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        List<Startup> myStartups = startupRepository.findByUserId(currentUser.getId());
        
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("milestone", new Milestone());
        model.addAttribute("today", LocalDate.now().toString());
        
        return "milestones/create";
    }

    // =============================================
    // CREATE MILESTONE
    // =============================================
    @PostMapping("/create")
    public String createMilestone(
            @RequestParam Long startupId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String targetDate,
            @RequestParam(required = false) String priority,
            RedirectAttributes redirectAttributes) {
        try {
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User currentUser = userRepository.findByEmail(email).orElse(null);
            
            Milestone milestone = new Milestone();
            milestone.setStartupId(startupId);
            milestone.setTitle(title);
            milestone.setDescription(description);
            
            if (targetDate != null && !targetDate.isEmpty()) {
                milestone.setTargetDate(LocalDateTime.parse(targetDate + "T00:00:00"));
            }
            
            milestone.setPriority(priority != null ? priority : "MEDIUM");
            milestone.setStatus("PENDING");
            milestone.setProgressPercentage(0);
            
            milestoneService.createMilestone(milestone);
            redirectAttributes.addFlashAttribute("success", "Milestone created successfully!");
            return "redirect:/milestones/" + startupId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create milestone: " + e.getMessage());
            return "redirect:/milestones/create";
        }
    }

    // =============================================
    // DELETE MILESTONE
    // =============================================
    @GetMapping("/{id}/delete")
    public String deleteMilestone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Milestone milestone = milestoneService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            Long startupId = milestone.getStartupId();
            milestoneService.deleteMilestone(id);
            redirectAttributes.addFlashAttribute("success", "Milestone deleted successfully!");
            return "redirect:/milestones/" + startupId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete milestone: " + e.getMessage());
            return "redirect:/milestones";
        }
    }

    // =============================================
    // MARK MILESTONE AS COMPLETE / UNDO COMPLETE
    // =============================================
    @GetMapping("/{id}/complete")
    public String completeMilestone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Milestone milestone = milestoneService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found with ID: " + id));
            
            if ("COMPLETED".equals(milestone.getStatus())) {
                milestone.setStatus("PENDING");
                milestone.setProgressPercentage(0);
                milestone.setCompletedAt(null);
                redirectAttributes.addFlashAttribute("success", "⏳ Milestone marked as pending!");
            } else {
                milestone.setStatus("COMPLETED");
                milestone.setProgressPercentage(100);
                milestone.setCompletedAt(LocalDateTime.now());
                redirectAttributes.addFlashAttribute("success", "✅ Milestone marked as completed!");
            }
            
            milestoneService.updateMilestone(milestone);
            return "redirect:/milestones/" + milestone.getStartupId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update milestone: " + e.getMessage());
            return "redirect:/milestones";
        }
    }

    // =============================================
    // EDIT MILESTONE FORM
    // =============================================
    @GetMapping("/{id}/edit")
    public String editMilestoneForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Milestone milestone = milestoneService.findById(id)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));
        
        model.addAttribute("milestone", milestone);
        model.addAttribute("userEmail", email);
        model.addAttribute("today", LocalDate.now().toString());
        
        return "milestones/edit";
    }

    // =============================================
    // UPDATE MILESTONE
    // =============================================
    @PostMapping("/{id}/edit")
    public String updateMilestone(@PathVariable Long id,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String targetDate,
                                  @RequestParam(required = false) String priority,
                                  RedirectAttributes redirectAttributes) {
        try {
            Milestone milestone = milestoneService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            milestone.setTitle(title);
            milestone.setDescription(description);
            
            if (targetDate != null && !targetDate.isEmpty()) {
                milestone.setTargetDate(LocalDateTime.parse(targetDate + "T00:00:00"));
            }
            
            milestone.setPriority(priority != null ? priority : "MEDIUM");
            
            milestoneService.updateMilestone(milestone);
            redirectAttributes.addFlashAttribute("success", "Milestone updated successfully!");
            return "redirect:/milestones/" + milestone.getStartupId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update milestone: " + e.getMessage());
            return "redirect:/milestones/" + id + "/edit";
        }
    }

    // =============================================
    // API: UPDATE PROGRESS (for AJAX calls)
    // =============================================
    @PostMapping("/api/{id}/progress")
    @ResponseBody
    public Map<String, Object> updateProgressApi(
            @PathVariable Long id,
            @RequestParam Integer progress,
            @RequestParam(required = false) String notes) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Milestone milestone = milestoneService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            milestone.setProgressPercentage(progress);
            milestone.setNotes(notes);
            milestone.setUpdatedAt(LocalDateTime.now());
            
            if (progress >= 100) {
                milestone.setStatus("COMPLETED");
                milestone.setCompletedAt(LocalDateTime.now());
            } else if (progress > 0) {
                milestone.setStatus("IN_PROGRESS");
            } else {
                milestone.setStatus("PENDING");
            }
            
            milestoneService.updateMilestone(milestone);
            
            response.put("success", true);
            response.put("message", "Progress updated successfully");
            response.put("milestone", milestone);
            return response;
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update progress: " + e.getMessage());
            return response;
        }
    }

    // =============================================
    // API: MARK COMPLETE (for AJAX calls)
    // =============================================
    @PostMapping("/api/{id}/complete")
    @ResponseBody
    public Map<String, Object> markCompleteApi(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Milestone milestone = milestoneService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            milestone.setStatus("COMPLETED");
            milestone.setProgressPercentage(100);
            milestone.setCompletedAt(LocalDateTime.now());
            milestone.setUpdatedAt(LocalDateTime.now());
            
            milestoneService.updateMilestone(milestone);
            
            response.put("success", true);
            response.put("message", "Milestone marked as complete");
            response.put("milestone", milestone);
            return response;
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to mark complete: " + e.getMessage());
            return response;
        }
    }

    // =============================================
    // API: DELETE MILESTONE (for AJAX calls)
    // =============================================
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public Map<String, Object> deleteMilestoneApi(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            milestoneService.deleteMilestone(id);
            response.put("success", true);
            response.put("message", "Milestone deleted successfully");
            return response;
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete milestone: " + e.getMessage());
            return response;
        }
    }
}