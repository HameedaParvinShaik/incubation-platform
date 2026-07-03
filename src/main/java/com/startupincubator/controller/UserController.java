package com.startupincubator.controller;

import com.startupincubator.entity.Role;
import com.startupincubator.entity.User;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.RoleRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.MentorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StartupRepository startupRepository;
    private final MentorRepository mentorRepository;
    private final PasswordEncoder passwordEncoder;

    // =============================================
    // LIST USERS
    // =============================================
    @GetMapping
    public String listUsers(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        List<User> users = userRepository.findAll();
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long pendingStartups = startupRepository.countByStatus(StartupStatus.PENDING);
        long totalMentors = mentorRepository.count();
        
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("pendingStartups", pendingStartups);
        model.addAttribute("totalMentors", totalMentors);
        model.addAttribute("userEmail", email);
        
        return "users/list";
    }

    // =============================================
    // GET USER BY ID (for View Modal)
    // =============================================
    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("isActive", user.getIsActive());
        response.put("createdAt", user.getCreatedAt());
        response.put("roles", user.getRoles());
        
        return response;
    }

    // =============================================
    // CREATE USER
    // =============================================
    @PostMapping("/create")
    public String createUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            @RequestParam(defaultValue = "true") boolean isActive,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (userRepository.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email already registered!");
                return "redirect:/users";
            }
            
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
                return "redirect:/users";
            }
            
            String roleName = "ROLE_" + role.toUpperCase();
            Role userRole = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            
            User user = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .password(passwordEncoder.encode(password))
                    .isActive(isActive)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }

    // =============================================
    // EDIT USER FORM
    // =============================================
    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Role> allRoles = roleRepository.findAll();
        
        model.addAttribute("user", user);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("userEmail", email);
        
        return "users/edit";
    }

    // =============================================
    // UPDATE USER
    // =============================================
    @PostMapping("/update/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber,
            @RequestParam String role,
            @RequestParam(defaultValue = "true") boolean isActive,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhoneNumber(phoneNumber);
            user.setIsActive(isActive);
            user.setUpdatedAt(LocalDateTime.now());
            
            String roleName = "ROLE_" + role.toUpperCase();
            Role userRole = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            
            userRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }

    // =============================================
    // DELETE USER
    // =============================================
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getEmail().equals("admin.incubator@platform.com")) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete the admin user!");
                return "redirect:/users";
            }
            
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
}