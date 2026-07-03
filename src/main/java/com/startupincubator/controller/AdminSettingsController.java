package com.startupincubator.controller;

import com.startupincubator.entity.SystemSetting;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.MentorRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.SystemSettingRepository;
import com.startupincubator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class AdminSettingsController {

    private final UserRepository userRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final SystemSettingRepository systemSettingRepository;

    private static final String DEFAULT_PLATFORM_NAME = "Startup Incubator Platform";
    private static final String DEFAULT_PLATFORM_EMAIL = "support@incubatorhub.com";
    private static final String DEFAULT_PLATFORM_DESCRIPTION = "Startup Incubation Platform connecting founders with mentors and investors.";

    @GetMapping
    public String settingsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        String platformName = getSetting("platform.name", DEFAULT_PLATFORM_NAME);
        String platformEmail = getSetting("platform.email", DEFAULT_PLATFORM_EMAIL);
        String platformDescription = getSetting("platform.description", DEFAULT_PLATFORM_DESCRIPTION);

        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorRepository.count();

        model.addAttribute("userEmail", email);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStartups", totalStartups);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("platformName", platformName);
        model.addAttribute("platformEmail", platformEmail);
        model.addAttribute("platformDescription", platformDescription);

        return "admin/settings/index";
    }

    @PostMapping("/general/update")
    public String updateGeneralSettings(
            @RequestParam String platformName,
            @RequestParam String platformEmail,
            @RequestParam(required = false) String platformDescription,
            RedirectAttributes redirectAttributes) {

        try {
            saveSetting("platform.name", platformName);
            saveSetting("platform.email", platformEmail);
            saveSetting("platform.description", platformDescription != null ? platformDescription : "");

            redirectAttributes.addFlashAttribute("success", "General settings updated successfully!");

        } catch (Exception e) {
            log.error("Error updating general settings: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update settings: " + e.getMessage());
        }

        return "redirect:/admin/settings";
    }

    private String getSetting(String key, String defaultValue) {
        try {
            return systemSettingRepository.findByKey(key)
                    .map(SystemSetting::getValue)
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.warn("Could not retrieve setting for key: {}, using default", key);
            return defaultValue;
        }
    }

    private void saveSetting(String key, String value) {
        try {
            SystemSetting setting = systemSettingRepository.findByKey(key)
                    .orElse(SystemSetting.builder()
                            .key(key)
                            .build());
            setting.setValue(value);
            systemSettingRepository.save(setting);
        } catch (Exception e) {
            log.error("Could not save setting for key: {}", key, e);
            throw new RuntimeException("Failed to save setting: " + key);
        }
    }
}