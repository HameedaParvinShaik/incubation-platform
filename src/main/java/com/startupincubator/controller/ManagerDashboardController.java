package com.startupincubator.controller;

import com.startupincubator.enums.StartupStatus;
import com.startupincubator.enums.FundingStatus;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.FundingRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager/dashboard")
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardController {

    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final FundingRequestRepository fundingRequestRepository;

    @GetMapping
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long totalMentors = mentorRepository.count();
        long pendingFunding = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long approvedFunding = fundingRequestRepository.countByStatus(FundingStatus.APPROVED);

        model.addAttribute("userEmail", email);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("pendingFunding", pendingFunding);
        model.addAttribute("approvedFunding", approvedFunding);
        model.addAttribute("recentStartups", startupRepository.findTop10ByOrderByCreatedAtDesc());

        return "manager/dashboard";
    }
}