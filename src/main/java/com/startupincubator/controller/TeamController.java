package com.startupincubator.controller;

import com.startupincubator.entity.Startup;
import com.startupincubator.entity.User;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final UserRepository userRepository;
    private final StartupRepository startupRepository;

    @GetMapping
    public String teamPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User currentUser = userRepository.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        // Get startups by this founder
        List<Startup> founderStartups = startupRepository.findByUserId(currentUser.getId());
        
        // Get all team members (for now just founder)
        List<User> teamMembers = new ArrayList<>();
        teamMembers.add(currentUser);
        
        // TODO: Add logic to fetch team members from startup_members table
        
        model.addAttribute("user", currentUser);
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("startups", founderStartups);
        
        return "team/list";
    }
}