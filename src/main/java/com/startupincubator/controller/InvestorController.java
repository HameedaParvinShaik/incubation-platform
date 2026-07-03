package com.startupincubator.controller;

import com.startupincubator.entity.FundingRequest;
import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.enums.FundingStatus;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.FundingRequestRepository;
import com.startupincubator.repository.StartupRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/investor")
@RequiredArgsConstructor
@Slf4j
public class InvestorController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final FundingRequestRepository fundingRequestRepository;

    @GetMapping("/dashboard")
    public String investorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        long totalStartups = startupRepository.count();
        long approvedStartups = startupRepository.countByStatus(StartupStatus.APPROVED);
        long activeStartups = startupRepository.countByStatus(StartupStatus.ACTIVE);
        
        User user = userRepository.findByEmail(email).orElse(null);
        long myInvestments = 0;
        if (user != null) {
            myInvestments = fundingRequestRepository.findByUserId(user.getId()).size();
        }
        
        List<Startup> topStartups = startupRepository.findByStatus(StartupStatus.APPROVED);
        
        for (Startup startup : topStartups) {
            List<FundingRequest> fundingRequests = fundingRequestRepository.findByStartupId(startup.getId());
            double startupFunding = fundingRequests.stream()
                    .filter(f -> f.getStatus() == FundingStatus.PENDING || 
                                 f.getStatus() == FundingStatus.APPROVED ||
                                 f.getStatus() == FundingStatus.FUNDED)
                    .mapToDouble(FundingRequest::getAmount)
                    .sum();
            startup.setTotalFundingAmount(startupFunding);
        }
        
        topStartups = topStartups.stream()
                .sorted((s1, s2) -> Double.compare(s2.getTotalFundingAmount(), s1.getTotalFundingAmount()))
                .limit(3)
                .collect(Collectors.toList());

        model.addAttribute("userEmail", email);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("approvedStartups", approvedStartups);
        model.addAttribute("activeStartups", activeStartups);
        model.addAttribute("myInvestments", myInvestments);
        model.addAttribute("topStartups", topStartups);

        return "dashboard/investor";
    }

    @GetMapping("/discover")
    public String discoverStartups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Model model) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        List<Startup> startups;
        
        if (search != null && !search.isEmpty()) {
            startups = startupRepository.findByNameContainingIgnoreCase(search);
        } else if (category != null && !category.isEmpty()) {
            startups = startupRepository.findAll().stream()
                    .filter(s -> category.equals(s.getCategory()))
                    .collect(Collectors.toList());
        } else {
            startups = startupRepository.findAll();
        }

        for (Startup startup : startups) {
            List<FundingRequest> fundingRequests = fundingRequestRepository.findByStartupId(startup.getId());
            double startupFunding = fundingRequests.stream()
                    .filter(f -> f.getStatus() == FundingStatus.PENDING || 
                                 f.getStatus() == FundingStatus.APPROVED ||
                                 f.getStatus() == FundingStatus.FUNDED)
                    .mapToDouble(FundingRequest::getAmount)
                    .sum();
            startup.setTotalFundingAmount(startupFunding);
        }

        List<String> categories = startupRepository.findAll().stream()
                .map(Startup::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("userEmail", email);
        model.addAttribute("startups", startups);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchTerm", search);

        return "investor/discover";
    }

    @GetMapping("/startups/{id}")
    public String startupDetails(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));

        List<FundingRequest> fundingRequests = fundingRequestRepository.findByStartupId(id);

        double totalFunded = fundingRequests.stream()
                .filter(f -> f.getStatus() == FundingStatus.PENDING || 
                             f.getStatus() == FundingStatus.APPROVED ||
                             f.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();
        startup.setTotalFundingAmount(totalFunded);

        model.addAttribute("startup", startup);
        model.addAttribute("fundingRequests", fundingRequests);
        model.addAttribute("userEmail", email);

        return "investor/startup-details";
    }

    // ✅ INVEST - Description is OPTIONAL
    @PostMapping("/invest")
    public String investInStartup(
            @RequestParam Long startupId,
            @RequestParam Double amount,
            @RequestParam String purpose,
            @RequestParam(required = false) String description,  // ✅ Optional
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/auth/login";
            }
            
            Startup startup = startupRepository.findById(startupId)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            if (startup.getStatus() != StartupStatus.APPROVED && startup.getStatus() != StartupStatus.ACTIVE) {
                redirectAttributes.addFlashAttribute("error", "This startup is not yet approved for investment!");
                return "redirect:/investor/discover";
            }
            
            FundingRequest fundingRequest = FundingRequest.builder()
                    .startupId(startupId)
                    .userId(user.getId())
                    .amount(amount)
                    .purpose(purpose)
                    .description(description != null ? description : "")  // ✅ Optional
                    .status(FundingStatus.PENDING)
                    .requestedBy(user.getFirstName() + " " + user.getLastName())
                    .requestedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            fundingRequestRepository.save(fundingRequest);
            
            log.info("💰 Investment request by {} for startup: {} amount: ${}", email, startup.getName(), amount);
            redirectAttributes.addFlashAttribute("success", "💰 Investment request submitted successfully! Waiting for manager approval.");
            
        } catch (Exception e) {
            log.error("Error creating investment request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to create investment request: " + e.getMessage());
        }
        
        return "redirect:/investor/dashboard";
    }

    @GetMapping("/investments")
    public String myInvestments(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<FundingRequest> investments = fundingRequestRepository.findByUserId(user.getId());

        long pendingCount = investments.stream()
                .filter(f -> f.getStatus() == FundingStatus.PENDING)
                .count();
        long approvedCount = investments.stream()
                .filter(f -> f.getStatus() == FundingStatus.APPROVED)
                .count();
        long fundedCount = investments.stream()
                .filter(f -> f.getStatus() == FundingStatus.FUNDED)
                .count();
        long rejectedCount = investments.stream()
                .filter(f -> f.getStatus() == FundingStatus.REJECTED)
                .count();
        
        double totalAmount = investments.stream()
                .filter(f -> f.getStatus() == FundingStatus.FUNDED)
                .mapToDouble(FundingRequest::getAmount)
                .sum();

        model.addAttribute("userEmail", email);
        model.addAttribute("investments", investments);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("fundedCount", fundedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalAmount", totalAmount);

        return "investor/investments";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("userEmail", email);

        return "investor/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("userEmail", email);

        return "investor/profile-edit";
    }

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
            return "redirect:/investor/profile";
            
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/investor/profile/edit";
        }
    }
}