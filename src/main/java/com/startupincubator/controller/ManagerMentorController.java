package com.startupincubator.controller;

import com.startupincubator.entity.Mentor;
import com.startupincubator.entity.MentorReview;
import com.startupincubator.entity.User;
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
@RequestMapping("/manager/mentors")
@RequiredArgsConstructor
@Slf4j
public class ManagerMentorController {

    private final MentorService mentorService;
    private final MentorRepository mentorRepository;
    private final MentorReviewRepository mentorReviewRepository;
    private final UserRepository userRepository;

    // =============================================
    // MANAGER MENTOR LIST
    // =============================================
    @GetMapping  // ← This now handles /manager/mentors
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
        long busyMentors = totalMentors - availableMentors;
        double avgRating = mentorService.getAverageRating();
        String formattedAvgRating = String.format("%.1f", avgRating);

        model.addAttribute("mentorWithUsers", mentorWithUsers);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("busyMentors", busyMentors);
        model.addAttribute("avgRating", formattedAvgRating);
        model.addAttribute("userEmail", email);

        return "manager/mentors/list";
    }

    // =============================================
    // MANAGER VIEW MENTOR
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

        return "manager/mentors/view";
    }

    // =============================================
    // MANAGER EDIT MENTOR FORM
    // =============================================
    @GetMapping("/{id}/edit")
    public String editMentorForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        model.addAttribute("mentor", mentor);
        model.addAttribute("userEmail", email);

        return "manager/mentors/edit";
    }

    // =============================================
    // MANAGER UPDATE MENTOR
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

        return "redirect:/manager/mentors";
    }

    // =============================================
    // MANAGER DELETE MENTOR
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

        return "redirect:/manager/mentors";
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