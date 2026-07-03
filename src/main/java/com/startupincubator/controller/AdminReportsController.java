package com.startupincubator.controller;

import com.startupincubator.entity.FundingRequest;
import com.startupincubator.entity.Mentor;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.FundingStatus;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.FundingRequestRepository;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class AdminReportsController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final FundingRequestRepository fundingRequestRepository;

    @GetMapping
    public String reportsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        long rejectedStartups = startupRepository.countByStatus(StartupStatus.REJECTED);
        long completedStartups = startupRepository.countByStatus(StartupStatus.COMPLETED);
        long totalUsers = userRepository.count();
        long totalMentors = mentorRepository.count();
        long totalFunding = fundingRequestRepository.count();
        long pendingFunding = fundingRequestRepository.countByStatus(FundingStatus.PENDING);
        long approvedFunding = fundingRequestRepository.countByStatus(FundingStatus.APPROVED);
        long fundedFunding = fundingRequestRepository.countByStatus(FundingStatus.FUNDED);
        long rejectedFunding = fundingRequestRepository.countByStatus(FundingStatus.REJECTED);

        model.addAttribute("userEmail", email);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("rejectedStartups", rejectedStartups);
        model.addAttribute("completedStartups", completedStartups);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("totalFunding", totalFunding);
        model.addAttribute("pendingFunding", pendingFunding);
        model.addAttribute("approvedFunding", approvedFunding);
        model.addAttribute("fundedFunding", fundedFunding);
        model.addAttribute("rejectedFunding", rejectedFunding);

        return "admin/reports/index";
    }
}