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
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final FundingRequestRepository fundingRequestRepository;

    @GetMapping
    public String reportsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        if (email == null || email.equals("anonymousUser")) {
            return "redirect:/auth/login";
        }

        // ✅ ALWAYS RETURN MANAGER REPORTS FOR MANAGER EMAIL
        if (email.equals("manager.incubator@platform.com")) {
            addAttributes(model, email);
            return "manager/reports";
        }

        // ✅ ALWAYS RETURN ADMIN REPORTS FOR ADMIN EMAIL
        if (email.equals("admin.incubator@platform.com")) {
            addAttributes(model, email);
            return "reports/index";
        }

        // Fallback
        addAttributes(model, email);
        return "reports/index";
    }

    private void addAttributes(Model model, String email) {
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
    }

    // =============================================
    // DOWNLOAD REPORTS AS CSV
    // =============================================
    
    @GetMapping("/startups/download")
    public ResponseEntity<byte[]> downloadStartupsReport() {
        List<Startup> startups = startupRepository.findAll();
        String csv = generateStartupsCSV(startups);
        return createDownloadResponse(csv, "startups_report_" + getTimestamp() + ".csv");
    }

    @GetMapping("/users/download")
    public ResponseEntity<byte[]> downloadUsersReport() {
        List<User> users = userRepository.findAll();
        String csv = generateUsersCSV(users);
        return createDownloadResponse(csv, "users_report_" + getTimestamp() + ".csv");
    }

    @GetMapping("/mentors/download")
    public ResponseEntity<byte[]> downloadMentorsReport() {
        List<Mentor> mentors = mentorRepository.findAll();
        String csv = generateMentorsCSV(mentors);
        return createDownloadResponse(csv, "mentors_report_" + getTimestamp() + ".csv");
    }

    @GetMapping("/funding/download")
    public ResponseEntity<byte[]> downloadFundingReport() {
        List<FundingRequest> fundingRequests = fundingRequestRepository.findAll();
        String csv = generateFundingCSV(fundingRequests);
        return createDownloadResponse(csv, "funding_report_" + getTimestamp() + ".csv");
    }

    private String generateStartupsCSV(List<Startup> startups) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,Category,Description,Status,Approved,User ID,Created At,Updated At\n");
        for (Startup s : startups) {
            sb.append(s.getId()).append(",")
              .append(escapeCSV(s.getName())).append(",")
              .append(escapeCSV(s.getCategory())).append(",")
              .append(escapeCSV(s.getDescription())).append(",")
              .append(s.getStatus()).append(",")
              .append(s.getIsApproved()).append(",")
              .append(s.getUserId()).append(",")
              .append(s.getCreatedAt()).append(",")
              .append(s.getUpdatedAt()).append("\n");
        }
        return sb.toString();
    }

    private String generateUsersCSV(List<User> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,First Name,Last Name,Email,Phone,Active,Created At,Updated At,Roles\n");
        for (User u : users) {
            String roles = u.getRoles().stream().map(r -> r.getName()).reduce((a, b) -> a + "|" + b).orElse("");
            sb.append(u.getId()).append(",")
              .append(escapeCSV(u.getFirstName())).append(",")
              .append(escapeCSV(u.getLastName())).append(",")
              .append(u.getEmail()).append(",")
              .append(escapeCSV(u.getPhoneNumber())).append(",")
              .append(u.getIsActive()).append(",")
              .append(u.getCreatedAt()).append(",")
              .append(u.getUpdatedAt()).append(",")
              .append(escapeCSV(roles)).append("\n");
        }
        return sb.toString();
    }

    private String generateMentorsCSV(List<Mentor> mentors) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,User ID,Expertise,Experience Years,Company,Designation,Available,Max Startups,Current Startups,Rating,Total Reviews\n");
        for (Mentor m : mentors) {
            sb.append(m.getId()).append(",")
              .append(m.getUserId()).append(",")
              .append(escapeCSV(m.getExpertise())).append(",")
              .append(m.getExperienceYears()).append(",")
              .append(escapeCSV(m.getCompany())).append(",")
              .append(escapeCSV(m.getDesignation())).append(",")
              .append(m.getAvailable()).append(",")
              .append(m.getMaxStartups()).append(",")
              .append(m.getCurrentStartups()).append(",")
              .append(m.getRating()).append(",")
              .append(m.getTotalReviews()).append("\n");
        }
        return sb.toString();
    }

    private String generateFundingCSV(List<FundingRequest> fundingRequests) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Startup ID,Startup Name,Amount,Purpose,Description,Status,Requested By,Requested At,Approved By,Approved At,Funded At,Transaction ID\n");
        for (FundingRequest f : fundingRequests) {
            String startupName = f.getStartup() != null ? f.getStartup().getName() : "N/A";
            sb.append(f.getId()).append(",")
              .append(f.getStartupId()).append(",")
              .append(escapeCSV(startupName)).append(",")
              .append(f.getAmount()).append(",")
              .append(escapeCSV(f.getPurpose())).append(",")
              .append(escapeCSV(f.getDescription())).append(",")
              .append(f.getStatus()).append(",")
              .append(escapeCSV(f.getRequestedBy())).append(",")
              .append(f.getRequestedAt()).append(",")
              .append(escapeCSV(f.getApprovedBy())).append(",")
              .append(f.getApprovedAt()).append(",")
              .append(f.getFundedAt()).append(",")
              .append(escapeCSV(f.getTransactionId())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private ResponseEntity<byte[]> createDownloadResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}