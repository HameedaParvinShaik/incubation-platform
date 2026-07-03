package com.startupincubator.controller;

import com.startupincubator.dto.RegisterRequest;
import com.startupincubator.entity.Role;
import com.startupincubator.entity.User;
import com.startupincubator.enums.RoleType;
import com.startupincubator.repository.RoleRepository;
import com.startupincubator.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password!");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully!");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(RegisterRequest request, RedirectAttributes redirectAttributes) {
        log.info("===== REGISTRATION ATTEMPT =====");
        log.info("Email: {}", request.getEmail());
        log.info("Selected Role: {}", request.getRole());

        // Check if email exists
        if (userService.existsByEmail(request.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Email already registered!");
            return "redirect:/auth/login";
        }

        // Check password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/auth/register";
        }

        try {
            // ✅ Get role from request, default to FOUNDER
            String roleName = request.getRole() != null && !request.getRole().isEmpty() 
                              ? request.getRole().toUpperCase() 
                              : "FOUNDER";
            
            // Convert to proper role name format (ROLE_ADMIN, ROLE_FOUNDER, etc.)
            String roleEnumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            
            log.info("Looking for role: {}", roleEnumName);
            
            Role defaultRole = roleRepository.findByName(roleEnumName)
                    .orElseGet(() -> {
                        log.warn("Role not found, creating it: {}", roleEnumName);
                        Role newRole = Role.builder()
                                .name(roleEnumName)
                                .description(roleName + " Role")
                                .build();
                        return roleRepository.save(newRole);
                    });

            log.info("Role found: {}", defaultRole.getName());

            // Create user
            User user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phoneNumber(request.getPhoneNumber())
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Set<Role> roles = new HashSet<>();
            roles.add(defaultRole);
            user.setRoles(roles);

            userService.registerUser(user);

            log.info("✅ User registered with role: {}", defaultRole.getName());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/auth/login?logout=true";
    }
}