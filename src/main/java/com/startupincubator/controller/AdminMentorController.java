package com.startupincubator.controller;

import com.startupincubator.entity.Mentor;
import com.startupincubator.entity.MentorReview;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.repository.MentorReviewRepository;
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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/mentors")
@RequiredArgsConstructor
@Slf4j
public class AdminMentorController {

    private final MentorService mentorService;
    private final MentorRepository mentorRepository;
    private final MentorReviewRepository mentorReviewRepository;
    private final UserRepository userRepository;

    // =============================================
    // ADMIN MENTOR LIST
    // =============================================
    @GetMapping
    public String listMentors(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

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
        String formattedAvgRating = String.format("%.1f", avgRating);

        model.addAttribute("mentorWithUsers", mentorWithUsers);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("avgRating", formattedAvgRating);
        model.addAttribute("userEmail", email);

        return "admin/mentors/list";
    }

    // =============================================
    // ADMIN VIEW MENTOR
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

        String formattedRating = String.format("%.1f", mentor.getRating() != null ? mentor.getRating() : 0.0);

        model.addAttribute("mentor", mentor);
        model.addAttribute("user", user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("formattedRating", formattedRating);
        model.addAttribute("userEmail", email);

        return "admin/mentors/view";
    }

    // =============================================
    // ADMIN EDIT MENTOR FORM
    // =============================================
    @GetMapping("/{id}/edit")
    public String editMentorForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        model.addAttribute("mentor", mentor);
        model.addAttribute("userEmail", email);

        return "admin/mentors/edit";
    }

    // =============================================
    // ADMIN UPDATE MENTOR
    // =============================================
    @PostMapping("/{id}/edit")
    public String updateMentor(@PathVariable Long id,
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

        } catch (Exception e) {
            log.error("Error updating mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update mentor: " + e.getMessage());
        }

        return "redirect:/admin/mentors";
    }

    // =============================================
    // ADMIN DELETE MENTOR
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

        } catch (Exception e) {
            log.error("Error deleting mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete mentor: " + e.getMessage());
        }

        return "redirect:/admin/mentors";
    }

    // =============================================
    // ✅ FIXED: ADMIN CREATE MENTOR FORM
    // =============================================
    @GetMapping("/create")
    public String createMentorForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // ✅ Get ALL users
        List<User> allUsers = userRepository.findAll();
        
        // ✅ Filter: Only users who are NOT already mentors
        List<User> availableUsers = new ArrayList<>();
        for (User user : allUsers) {
            // Check if this user already has a mentor profile
            boolean isMentor = mentorRepository.findByUserId(user.getId()).isPresent();
            if (!isMentor) {
                availableUsers.add(user);
            }
        }

        // ✅ LOG for debugging
        log.info("=== CREATE MENTOR FORM ===");
        log.info("Total users: {}", allUsers.size());
        log.info("Available users (not mentors): {}", availableUsers.size());
        for (User u : availableUsers) {
            log.info("  - {} {} ({})", u.getFirstName(), u.getLastName(), u.getEmail());
        }

        model.addAttribute("mentor", new Mentor());
        model.addAttribute("availableUsers", availableUsers);
        model.addAttribute("userEmail", email);

        return "admin/mentors/create";
    }

    // =============================================
    // ADMIN CREATE MENTOR - Save
    // =============================================
    @PostMapping("/create")
    public String createMentor(@ModelAttribute Mentor mentor,
                               @RequestParam Long userId,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Set mentor name from user
            mentor.setUserId(userId);
            mentor.setName(user.getFirstName() + " " + user.getLastName());
            mentor.setAvailable(true);
            mentor.setCurrentStartups(0);
            mentor.setRating(0.0);
            mentor.setTotalReviews(0);

            mentorRepository.save(mentor);

            log.info("✅ Mentor created: {} - {}", mentor.getName(), mentor.getExpertise());
            redirectAttributes.addFlashAttribute("success", "Mentor created successfully!");

        } catch (Exception e) {
            log.error("Error creating mentor: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to create mentor: " + e.getMessage());
        }

        return "redirect:/admin/mentors";
    }

    // =============================================
    // REDIRECT old /mentor/admin-list to /admin/mentors
    // =============================================
    @GetMapping("/admin-list")
    public String redirectAdminList() {
        return "redirect:/admin/mentors";
    }

    // =============================================
    // INNER CLASS
    // =============================================
    public static class MentorWithUser {
        private Mentor mentor;
        private User user;

        public Mentor getMentor() { return mentor; }
        public void setMentor(Mentor mentor) { this.mentor = mentor; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }
}