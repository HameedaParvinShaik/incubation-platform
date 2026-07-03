package com.startupincubator.controller;

import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.service.MentorService;
import com.startupincubator.service.StartupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final StartupService startupService;
    private final MentorService mentorService;
    private final UserRepository userRepository;
    private final StartupRepository startupRepository;

    // =============================================
    // ADMIN DASHBOARD DIRECT ACCESS - ADD THIS
    // =============================================
    @GetMapping("/dashboard/admin")
    public String adminDashboardDirect(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorService.countMentors();
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        
        model.addAttribute("userName", "Admin");
        model.addAttribute("userEmail", email);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("recentStartups", startupService.findRecentStartups());
        
        return "dashboard/admin";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        log.info("===== DASHBOARD ACCESS =====");
        log.info("User: {}", email);
        
        String role = "NO_ROLE";
        if (!authorities.isEmpty()) {
            role = authorities.iterator().next().getAuthority();
        }
        
        log.info("Extracted Role: {}", role);
        
        // ✅ Always set userEmail in the model
        model.addAttribute("userEmail", email);
        model.addAttribute("userRole", role);

        String viewName;
        
        // Forced by email (for testing)
        if (email.equals("founder.incubator@platform.com")) {
            log.info("🔴 FORCED: Founder dashboard");
            viewName = founderDashboard(model, email);
        } else if (email.equals("admin.incubator@platform.com")) {
            log.info("🔴 FORCED: Admin dashboard");
            viewName = adminDashboard(model);
        } else if (email.equals("manager.incubator@platform.com")) {
            log.info("🔴 FORCED: Manager dashboard");
            viewName = managerDashboard(model);
        } else if (email.equals("mentor.incubator@platform.com")) {
            log.info("🔴 FORCED: Mentor dashboard");
            viewName = mentorDashboard(model);
        } else if (email.equals("evaluator.incubator@platform.com")) {
            log.info("🔴 FORCED: Evaluator dashboard");
            viewName = evaluatorDashboard(model);
        } else if (email.equals("investor.incubator@platform.com")) {
            log.info("🔴 FORCED: Investor dashboard");
            viewName = investorDashboard(model);
        } else if (email.equals("team.incubator@platform.com")) {
            log.info("🔴 FORCED: Team Member dashboard");
            viewName = teamMemberDashboard(model, email);
        } else {
            log.info("🔄 Using switch for role: {}", role);
            switch (role) {
                case "ROLE_ADMIN":
                    viewName = adminDashboard(model);
                    break;
                case "ROLE_MANAGER":
                    viewName = managerDashboard(model);
                    break;
                case "ROLE_MENTOR":
                    viewName = mentorDashboard(model);
                    break;
                case "ROLE_FOUNDER":
                    viewName = founderDashboard(model, email);
                    break;
                case "ROLE_EVALUATOR":
                    viewName = evaluatorDashboard(model);
                    break;
                case "ROLE_INVESTOR":
                    viewName = investorDashboard(model);
                    break;
                case "ROLE_TEAM_MEMBER":
                    viewName = teamMemberDashboard(model, email);
                    break;
                default:
                    log.warn("⚠️ No matching role found: {}", role);
                    viewName = "redirect:/auth/login";
                    break;
            }
        }
        
        return viewName;
    }

    // =============================================
    // ADMIN DASHBOARD
    // =============================================
    private String adminDashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorService.countMentors();
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);

        model.addAttribute("userName", "Admin");
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("recentStartups", startupService.findRecentStartups());

        return "dashboard/admin";
    }

    // =============================================
    // MANAGER DASHBOARD
    // =============================================
    private String managerDashboard(Model model) {
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long totalMentors = mentorService.countMentors();

        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("recentStartups", startupService.findRecentStartups());

        return "dashboard/manager";
    }

    // =============================================
    // MENTOR DASHBOARD
    // =============================================
    private String mentorDashboard(Model model) {
        long totalMentors = mentorService.countMentors();
        long availableMentors = mentorService.countAvailableMentors();
        double avgRating = mentorService.getAverageRating();

        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("availableMentors", availableMentors);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("mentors", mentorService.findAvailableMentors());

        return "dashboard/mentor";
    }

    // =============================================
    // FOUNDER DASHBOARD
    // =============================================
    private String founderDashboard(Model model, String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.error("❌ User not found: {}", email);
            return "redirect:/auth/login";
        }

        List<Startup> myStartups = startupService.findByUserId(user.getId());
        long myStartupsCount = myStartups.size();
        long approvedCount = myStartups.stream().filter(s -> s.getIsApproved() != null && s.getIsApproved()).count();
        long pendingCount = myStartupsCount - approvedCount;

        model.addAttribute("myStartups", myStartups);
        model.addAttribute("myStartupsCount", myStartupsCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());

        return "dashboard/founder";
    }

    // =============================================
    // EVALUATOR DASHBOARD
    // =============================================
    private String evaluatorDashboard(Model model) {
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        long totalStartups = startupRepository.count();

        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("startups", startupService.findByStatus(StartupStatus.PENDING));

        return "dashboard/evaluator";
    }

    // =============================================
    // INVESTOR DASHBOARD
    // =============================================
    private String investorDashboard(Model model) {
        long totalStartups = startupRepository.count();
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);

        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("startups", startupService.findApprovedStartups());

        return "dashboard/investor";
    }

    // =============================================
    // TEAM MEMBER DASHBOARD
    // =============================================
    private String teamMemberDashboard(Model model, String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        List<Startup> myStartups = startupService.findByUserId(user.getId());

        model.addAttribute("myStartups", myStartups);
        model.addAttribute("myStartupsCount", myStartups.size());
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());

        return "dashboard/team-member";
    }
}