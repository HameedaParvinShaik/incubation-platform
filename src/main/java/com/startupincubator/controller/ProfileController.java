package com.startupincubator.controller;

import com.startupincubator.entity.User;
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
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    public String profilePage(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            if (email == null || email.equals("anonymousUser")) {
                return "redirect:/auth/login";
            }
            
            log.info("🔵 Profile page accessed by: {}", email);
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.error("❌ User not found: {}", email);
                return "redirect:/auth/login";
            }
            
            // Check user role
            String role = auth.getAuthorities().iterator().next().getAuthority();
            boolean isFounder = role.equals("ROLE_FOUNDER");
            boolean isAdmin = role.equals("ROLE_ADMIN");
            boolean isManager = role.equals("ROLE_MANAGER");
            
            model.addAttribute("user", user);
            model.addAttribute("userEmail", email);
            model.addAttribute("userRole", role);
            model.addAttribute("isFounder", isFounder);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isManager", isManager);
            
            log.info("✅ Profile loaded for user: {}, Role: {}", email, role);
            
            // Return the correct template based on role
            if (isFounder) {
                return "profile/founder";
            } else if (isManager) {
                return "profile/manager";
            } else if (isAdmin) {
                return "profile/admin";
            }
            return "profile/index";
            
        } catch (Exception e) {
            log.error("❌ Error loading profile: {}", e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            if (email == null || email.equals("anonymousUser")) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            String role = auth.getAuthorities().iterator().next().getAuthority();
            boolean isManager = role.equals("ROLE_MANAGER");
            
            model.addAttribute("user", user);
            model.addAttribute("userEmail", email);
            model.addAttribute("isManager", isManager);
            
            // Return correct edit page
            if (isManager) {
                return "profile/manager-edit";
            }
            return "profile/edit";
            
        } catch (Exception e) {
            log.error("❌ Error loading edit profile: {}", e.getMessage());
            return "redirect:/profile";
        }
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phoneNumber,
            RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            if (email == null || email.equals("anonymousUser")) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(phoneNumber);
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            
            // ✅ Redirect based on role
            String role = auth.getAuthorities().iterator().next().getAuthority();
            if (role.equals("ROLE_MANAGER")) {
                return "redirect:/profile";  // This will go to the manager profile page
            } else if (role.equals("ROLE_ADMIN")) {
                return "redirect:/profile";
            } else if (role.equals("ROLE_FOUNDER")) {
                return "redirect:/profile";
            }
            return "redirect:/profile";
            
        } catch (Exception e) {
            log.error("❌ Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/profile";
        }
    }
}