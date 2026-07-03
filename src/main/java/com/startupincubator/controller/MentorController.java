package com.startupincubator.controller;

import com.startupincubator.entity.Mentor;
import com.startupincubator.entity.MentorReview;
import com.startupincubator.entity.Milestone;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.repository.MentorReviewRepository;
import com.startupincubator.repository.MilestoneRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.service.MentorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mentor")
@RequiredArgsConstructor
@Slf4j
public class MentorController {

    private final MentorService mentorService;
    private final MentorRepository mentorRepository;
    private final MentorReviewRepository mentorReviewRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;

    // =============================================
    // REDIRECT: /mentor -> /mentor/dashboard
    // =============================================
    @GetMapping
    public String redirectToMentorDashboard() {
        return "redirect:/mentor/dashboard";
    }

    // =============================================
    // /mentors -> /mentor/list (for admin sidebar)
    // =============================================
    @GetMapping("/mentors")
    public String redirectToMentorList() {
        return "redirect:/mentor/list";
    }

    // =============================================
    // LIST ALL MENTORS - Uses mentor/list.html (MENTOR VIEW)
    // =============================================
    @GetMapping("/list")
    public String mentorList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        List<Mentor> mentors = mentorRepository.findAll();
        
        List<MentorWithUser> mentorWithUsers = new ArrayList<>();
        for (Mentor mentor : mentors) {
            MentorWithUser mwu = new MentorWithUser();
            mwu.setMentor(mentor);
            if (mentor.getUserId() != null) {
                User user = userRepository.findById(mentor.getUserId()).orElse(null);
                mwu.setUser(user);
            }
            mentorWithUsers.add(mwu);
        }
        
        long totalMentors = mentorRepository.count();
        long availableMentors = mentorRepository.countByAvailableTrue();
        double avgRating = mentorService.getAverageRating();

        addCommonAttributes(model);
        
        model.addAttribute("mentorWithUsers", mentorWithUsers);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("userEmail", email);
        
        return "mentor/list";
    }

    // =============================================
    // CREATE MENTOR - Show form (MENTOR VIEW)
    // =============================================
    @GetMapping("/create")
    public String createMentorForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }
        
        List<User> availableUsers = userRepository.findAll();
        List<User> mentorUsers = new ArrayList<>();
        
        for (User user : availableUsers) {
            boolean isMentor = mentorRepository.findByUserId(user.getId()).isPresent();
            if (!isMentor) {
                mentorUsers.add(user);
            }
        }
        
        addCommonAttributes(model);
        model.addAttribute("mentor", new Mentor());
        model.addAttribute("availableUsers", mentorUsers);
        model.addAttribute("userEmail", email);
        
        return "mentor/create";
    }

    // =============================================
    // CREATE MENTOR - Save (MENTOR VIEW)
    // =============================================
    @PostMapping("/create")
    public String createMentor(@ModelAttribute Mentor mentor,
                               @RequestParam Long userId,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            mentor.setUserId(userId);
            mentor.setAvailable(true);
            mentor.setCurrentStartups(0);
            mentor.setRating(0.0);
            mentor.setTotalReviews(0);
            
            mentorRepository.save(mentor);
            
            redirectAttributes.addFlashAttribute("success", "Mentor created successfully!");
            return "redirect:/mentor/list";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create mentor: " + e.getMessage());
            return "redirect:/mentor/create";
        }
    }

    // =============================================
    // VIEW MENTOR - With Reviews
    // =============================================
    @GetMapping("/{id}")
    public String viewMentor(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        
        User user = null;
        if (mentor.getUserId() != null) {
            user = userRepository.findById(mentor.getUserId()).orElse(null);
        }
        
        List<MentorReview> reviews = mentorReviewRepository.findByMentorId(id);
        
        User currentUser = userRepository.findByEmail(email).orElse(null);
        boolean hasReviewed = false;
        if (currentUser != null) {
            hasReviewed = mentorReviewRepository.existsByMentorIdAndReviewerId(id, currentUser.getId());
        }
        
        addCommonAttributes(model);
        model.addAttribute("mentor", mentor);
        model.addAttribute("user", user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("userEmail", email);
        
        return "mentor/view";
    }

    // =============================================
    // SUBMIT REVIEW / RATING
    // =============================================
    @PostMapping("/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User reviewer = userRepository.findByEmail(email).orElse(null);
            if (reviewer == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/auth/login";
            }
            
            Mentor mentor = mentorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mentor not found"));
            
            if (mentorReviewRepository.existsByMentorIdAndReviewerId(id, reviewer.getId())) {
                redirectAttributes.addFlashAttribute("error", "You have already reviewed this mentor!");
                return "redirect:/mentor/" + id;
            }
            
            MentorReview review = MentorReview.builder()
                    .mentor(mentor)
                    .reviewer(reviewer)
                    .rating(rating)
                    .comment(comment)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            mentorReviewRepository.save(review);
            
            // ✅ FIXED: Recalculate rating manually
            List<MentorReview> allReviews = mentorReviewRepository.findByMentorId(id);
            double avgRating = allReviews.stream()
                    .mapToInt(MentorReview::getRating)
                    .average()
                    .orElse(0.0);
            mentor.setRating(avgRating);
            mentor.setTotalReviews(allReviews.size());
            mentorRepository.save(mentor);
            
            redirectAttributes.addFlashAttribute("success", "Review submitted successfully! Thank you for your feedback.");
            
        } catch (Exception e) {
            log.error("Error submitting review: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to submit review: " + e.getMessage());
        }
        return "redirect:/mentor/" + id;
    }

    // =============================================
    // EDIT MENTOR - Show form (ADMIN)
    // =============================================
    @GetMapping("/{id}/edit")
    public String editMentorForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        
        addCommonAttributes(model);
        model.addAttribute("mentor", mentor);
        model.addAttribute("userEmail", email);
        model.addAttribute("userRole", "ROLE_ADMIN");
        
        return "mentor/edit";
    }

    // =============================================
    // EDIT MENTOR - Update (BOTH ADMIN & MANAGER)
    // =============================================
    @PostMapping("/{id}/edit")
    public String editMentor(@PathVariable Long id,
                             @ModelAttribute Mentor updatedMentor,
                             RedirectAttributes redirectAttributes) {
        try {
            Mentor existingMentor = mentorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mentor not found"));
            
            existingMentor.setExpertise(updatedMentor.getExpertise());
            existingMentor.setExperienceYears(updatedMentor.getExperienceYears());
            existingMentor.setCompany(updatedMentor.getCompany());
            existingMentor.setDesignation(updatedMentor.getDesignation());
            existingMentor.setBio(updatedMentor.getBio());
            existingMentor.setAvailable(updatedMentor.getAvailable());
            existingMentor.setMaxStartups(updatedMentor.getMaxStartups());
            
            mentorRepository.save(existingMentor);
            
            redirectAttributes.addFlashAttribute("success", "Mentor updated successfully!");
            
            return "redirect:/mentor/admin-list";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update mentor: " + e.getMessage());
            return "redirect:/mentor/" + id + "/edit";
        }
    }

    // =============================================
    // DELETE MENTOR
    // =============================================
    @PostMapping("/{id}/delete")
    public String deleteMentor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Mentor mentor = mentorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mentor not found"));
            
            List<MentorReview> reviews = mentorReviewRepository.findByMentorId(id);
            mentorReviewRepository.deleteAll(reviews);
            
            mentorRepository.delete(mentor);
            
            redirectAttributes.addFlashAttribute("success", "Mentor deleted successfully!");
            log.info("🗑️ Mentor deleted: {}", mentor.getExpertise());
            
        } catch (Exception e) {
            log.error("Error deleting mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete mentor: " + e.getMessage());
        }
        
        return "redirect:/mentor/admin-list";
    }

    // =============================================
    // DASHBOARD - Uses dashboard/mentor.html
    // =============================================
    @GetMapping("/dashboard")
    public String mentorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        long totalMentors = mentorRepository.count();
        long availableMentors = mentorRepository.countByAvailableTrue();
        double avgRating = mentorService.getAverageRating();

        List<Startup> assignedStartups = new ArrayList<>();
        if (mentor != null) {
            // ✅ FIXED: Use mentor.getId() instead of mentor.getUserId()
            assignedStartups = startupRepository.findByMentorId(mentor.getId());
        }

        model.addAttribute("userEmail", email);
        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("myMentees", assignedStartups.size());
        model.addAttribute("assignedStartups", assignedStartups);
        model.addAttribute("mentors", mentorRepository.findByAvailableTrue());

        return "dashboard/mentor";
    }

    // =============================================
    // MY MENTEES - Uses mentor/mentees.html
    // =============================================
    @GetMapping("/mentees")
    public String myMentees(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        List<Startup> startups = new ArrayList<>();
        if (mentor != null) {
            // ✅ FIXED: Use mentor.getId() instead of mentor.getUserId()
            startups = startupRepository.findByMentorId(mentor.getId());
        }

        model.addAttribute("startups", startups);
        model.addAttribute("userEmail", email);
        model.addAttribute("myMenteesCount", startups.size());
        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);

        return "mentor/mentees";
    }

    // =============================================
    // MENTEE DETAILS
    // =============================================
    @GetMapping("/mentees/{id}")
    public String viewMentee(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
        
        model.addAttribute("startup", startup);
        model.addAttribute("userEmail", email);
        
        return "mentor/mentee-details";
    }

    // =============================================
    // SESSIONS - Uses mentor/sessions.html
    // =============================================
    @GetMapping("/sessions")
    public String sessions(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        List<Startup> assignedStartups = new ArrayList<>();
        if (mentor != null) {
            // ✅ FIXED: Use mentor.getId() instead of mentor.getUserId()
            assignedStartups = startupRepository.findByMentorId(mentor.getId());
            log.info("📋 Mentor {} has {} assigned startups", email, assignedStartups.size());
        }

        model.addAttribute("userEmail", email);
        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);
        model.addAttribute("assignedStartups", assignedStartups);
        model.addAttribute("myMenteesCount", assignedStartups.size());

        return "mentor/sessions";
    }

    // =============================================
    // SCHEDULE SESSION
    // =============================================
    @PostMapping("/sessions/schedule")
    public String scheduleSession(
            @RequestParam Long startupId,
            @RequestParam String title,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String agenda,
            @RequestParam(required = false) String meetingLink,
            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/auth/login";
            }
            
            Mentor mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
            if (mentor == null) {
                redirectAttributes.addFlashAttribute("error", "Mentor profile not found!");
                return "redirect:/mentor/dashboard";
            }
            
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            redirectAttributes.addFlashAttribute("success", "Session scheduled successfully for " + startup.getName());
            
        } catch (Exception e) {
            log.error("Error scheduling session: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to schedule session: " + e.getMessage());
        }
        return "redirect:/mentor/sessions";
    }

    // =============================================
    // REVIEWS - Uses mentor/reviews.html
    // =============================================
    @GetMapping("/reviews")
    public String reviews(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        List<Startup> startups = new ArrayList<>();
        if (mentor != null) {
            // ✅ FIXED: Use mentor.getId() instead of mentor.getUserId()
            startups = startupRepository.findByMentorId(mentor.getId());
        }

        model.addAttribute("startups", startups);
        model.addAttribute("userEmail", email);
        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);
        model.addAttribute("myMenteesCount", startups.size());

        return "mentor/reviews";
    }

    // =============================================
    // SUBMIT REVIEW (from mentor/reviews.html)
    // =============================================
    @PostMapping("/reviews/{startupId}")
    public String submitReviewFromMentor(@PathVariable Long startupId,
                                         @RequestParam String review,
                                         @RequestParam Integer rating,
                                         RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            redirectAttributes.addFlashAttribute("success", "Review submitted successfully!");
            log.info("✅ Review submitted for startup: {} by mentor: {}", startup.getName(), email);
            
        } catch (Exception e) {
            log.error("Error submitting review: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to submit review: " + e.getMessage());
        }
        return "redirect:/mentor/reviews";
    }

    // =============================================
    // PROFILE - Uses mentor/profile.html
    // =============================================
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);
        model.addAttribute("userEmail", email);

        return "mentor/profile";
    }

    // =============================================
    // MILESTONES - Uses mentor/milestones.html
    // =============================================
    @GetMapping("/milestones")
    public String milestones(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        Mentor mentor = null;
        if (user != null) {
            mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
        }

        List<Startup> assignedStartups = new ArrayList<>();
        List<Milestone> milestones = new ArrayList<>();
        
        if (mentor != null) {
            // ✅ FIXED: Use mentor.getId() instead of mentor.getUserId()
            assignedStartups = startupRepository.findByMentorId(mentor.getId());
            milestones = milestoneRepository.findByMentorId(mentor.getId());
        }

        long totalMilestones = milestones.size();
        long completedMilestones = milestones.stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
        long inProgressMilestones = milestones.stream().filter(m -> "IN_PROGRESS".equals(m.getStatus())).count();
        int completionRate = totalMilestones > 0 ? (int) ((completedMilestones * 100) / totalMilestones) : 0;

        model.addAttribute("userEmail", email);
        model.addAttribute("user", user);
        model.addAttribute("mentor", mentor);
        model.addAttribute("assignedStartups", assignedStartups);
        model.addAttribute("milestones", milestones);
        model.addAttribute("totalMilestones", totalMilestones);
        model.addAttribute("completedMilestones", completedMilestones);
        model.addAttribute("inProgressMilestones", inProgressMilestones);
        model.addAttribute("completionRate", completionRate);

        return "mentor/milestones";
    }

    // =============================================
    // CREATE MILESTONE - API
    // =============================================
    @PostMapping("/milestones/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createMilestone(
            @RequestParam Long startupId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String targetDate,
            @RequestParam(required = false) String priority) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Mentor mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
            if (mentor == null) {
                response.put("success", false);
                response.put("message", "Mentor not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            Milestone milestone = Milestone.builder()
                    .startupId(startupId)
                    .mentorId(mentor.getId())
                    .title(title)
                    .description(description)
                    .targetDate(LocalDateTime.parse(targetDate + "T00:00:00"))
                    .status("PENDING")
                    .progressPercentage(0)
                    .priority(priority != null ? priority : "MEDIUM")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            milestoneRepository.save(milestone);
            
            response.put("success", true);
            response.put("message", "Milestone created successfully");
            response.put("milestone", milestone);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating milestone: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to create milestone: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =============================================
    // UPDATE MILESTONE PROGRESS - API
    // =============================================
    @PostMapping("/milestones/{id}/progress")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress,
            @RequestParam(required = false) String notes) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Milestone milestone = milestoneRepository.findById(id)
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
            
            milestoneRepository.save(milestone);
            
            response.put("success", true);
            response.put("message", "Progress updated successfully");
            response.put("milestone", milestone);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating progress: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to update progress: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =============================================
    // MARK MILESTONE COMPLETE - API
    // =============================================
    @PostMapping("/milestones/{id}/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markComplete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Milestone milestone = milestoneRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            
            milestone.setStatus("COMPLETED");
            milestone.setProgressPercentage(100);
            milestone.setCompletedAt(LocalDateTime.now());
            milestone.setUpdatedAt(LocalDateTime.now());
            
            milestoneRepository.save(milestone);
            
            response.put("success", true);
            response.put("message", "Milestone marked as complete");
            response.put("milestone", milestone);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error marking complete: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to mark complete: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =============================================
    // DELETE MILESTONE - API
    // =============================================
    @DeleteMapping("/milestones/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteMilestone(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            milestoneRepository.deleteById(id);
            response.put("success", true);
            response.put("message", "Milestone deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting milestone: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to delete milestone: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =============================================
    // HELPER METHOD - Add Common Attributes
    // =============================================
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

    // =============================================
    // INNER CLASS - MentorWithUser
    // =============================================
    public static class MentorWithUser {
        private Mentor mentor;
        private User user;

        public Mentor getMentor() { return mentor; }
        public void setMentor(Mentor mentor) { this.mentor = mentor; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }

    // =============================================
    // ADMIN MENTOR LIST - Uses mentor/admin-list.html
    // =============================================
    @GetMapping("/admin-list")
    public String adminMentorList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        String userRole = auth.getAuthorities().iterator().next().getAuthority();

        List<Mentor> mentors = mentorRepository.findAll();
        
        List<MentorWithUser> mentorWithUsers = new ArrayList<>();
        for (Mentor mentor : mentors) {
            MentorWithUser mwu = new MentorWithUser();
            mwu.setMentor(mentor);
            if (mentor.getUserId() != null) {
                User user = userRepository.findById(mentor.getUserId()).orElse(null);
                mwu.setUser(user);
            }
            mentorWithUsers.add(mwu);
        }
        
        long totalMentors = mentorRepository.count();
        long availableMentors = mentorRepository.countByAvailableTrue();
        double avgRating = mentorService.getAverageRating();

        addCommonAttributes(model);
        
        model.addAttribute("mentorWithUsers", mentorWithUsers);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("userEmail", email);
        model.addAttribute("userRole", userRole);
        
        return "mentor/admin-list";
    }

    // =============================================
    // ADMIN CREATE MENTOR - Show form
    // =============================================
    @GetMapping("/admin-create")
    public String adminCreateMentorForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }
        
        List<User> availableUsers = userRepository.findAll();
        List<User> mentorUsers = new ArrayList<>();
        
        for (User user : availableUsers) {
            boolean isMentor = mentorRepository.findByUserId(user.getId()).isPresent();
            if (!isMentor) {
                mentorUsers.add(user);
            }
        }
        
        addCommonAttributes(model);
        model.addAttribute("mentor", new Mentor());
        model.addAttribute("availableUsers", mentorUsers);
        model.addAttribute("userEmail", email);
        
        return "mentor/admin-create";
    }

    // =============================================
    // ADMIN CREATE MENTOR - Save
    // =============================================
    @PostMapping("/admin-create")
    public String adminCreateMentor(@ModelAttribute Mentor mentor,
                                    @RequestParam Long userId,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            mentor.setUserId(userId);
            mentor.setAvailable(true);
            mentor.setCurrentStartups(0);
            mentor.setRating(0.0);
            mentor.setTotalReviews(0);
            
            mentorRepository.save(mentor);
            
            redirectAttributes.addFlashAttribute("success", "Mentor created successfully!");
            return "redirect:/mentor/admin-list";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create mentor: " + e.getMessage());
            return "redirect:/mentor/admin-create";
        }
    }

    // =============================================
    // MANAGER EDIT MENTOR - Show form
    // =============================================
    @GetMapping("/manager-edit/{id}")
    public String managerEditMentorForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        
        addCommonAttributes(model);
        model.addAttribute("mentor", mentor);
        model.addAttribute("userEmail", email);
        model.addAttribute("userRole", "ROLE_MANAGER");
        
        return "mentor/manager-edit";
    }
}