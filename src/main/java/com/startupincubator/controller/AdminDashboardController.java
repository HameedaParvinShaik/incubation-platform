package com.startupincubator.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;

    @GetMapping
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        long completedStartups = startupRepository.countByStatus(StartupStatus.COMPLETED);
        long totalMentors = mentorRepository.count();

        model.addAttribute("userEmail", email);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("completedStartups", completedStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("recentStartups", startupRepository.findTop10ByOrderByCreatedAtDesc());

        return "admin/dashboard";
    }
}