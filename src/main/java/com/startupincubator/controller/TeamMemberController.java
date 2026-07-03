package com.startupincubator.controller;

import com.startupincubator.entity.Startup;
import com.startupincubator.entity.Task;
import com.startupincubator.entity.User;
import com.startupincubator.repository.StartupRepository;
import com.startupincubator.repository.TaskRepository;
import com.startupincubator.repository.UserRepository;
import com.startupincubator.service.StartupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/team-member")
@RequiredArgsConstructor
@Slf4j
public class TeamMemberController {

    private final StartupService startupService;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // =============================================
    // DASHBOARD
    // =============================================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        List<Startup> myStartups = List.of();
        String userName = "Team Member";
        Long userId = null;
        
        if (user != null) {
            userId = user.getId();
            myStartups = startupService.findByUserId(userId);
            userName = user.getFirstName() + " " + user.getLastName();
        }

        // Get tasks from database
        List<Task> tasks = new ArrayList<>();
        long tasksDone = 0;
        long pendingTasks = 0;
        
        if (userId != null) {
            tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
            tasksDone = taskRepository.countByUserIdAndStatus(userId, "COMPLETED");
            pendingTasks = tasks.size() - tasksDone;
        }

        model.addAttribute("userEmail", email);
        model.addAttribute("userName", userName);
        model.addAttribute("myStartups", myStartups);
        model.addAttribute("myStartupsCount", myStartups.size());
        model.addAttribute("tasksDone", tasksDone);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("teamMembers", 5);

        return "dashboard/team-member";
    }

    // =============================================
    // MY TASKS
    // =============================================
    @GetMapping("/tasks")
    public String tasks(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);
        List<Task> tasks = new ArrayList<>();
        long totalTasks = 0;
        long completedTasks = 0;
        long pendingTasks = 0;
        
        if (user != null) {
            tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            totalTasks = tasks.size();
            completedTasks = taskRepository.countByUserIdAndStatus(user.getId(), "COMPLETED");
            pendingTasks = totalTasks - completedTasks;
        }

        model.addAttribute("userEmail", email);
        model.addAttribute("tasks", tasks);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);

        return "team-member/tasks";
    }

    // =============================================
    // CREATE TASK
    // =============================================
    @PostMapping("/tasks/create")
    public String createTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String priority,
            @RequestParam(required = false) String dueDate,
            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "❌ User not found!");
                return "redirect:/team-member/tasks";
            }
            
            Task task = Task.builder()
                    .title(title)
                    .description(description != null ? description : "")
                    .priority(priority)
                    .status("PENDING")
                    .dueDate(dueDate)
                    .userId(user.getId())
                    .assignedTo(email)
                    .build();
            
            taskRepository.save(task);
            
            log.info("✅ Task created by {}: {}", email, title);
            redirectAttributes.addFlashAttribute("success", "✅ Task '" + title + "' created successfully!");
            
        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to create task: " + e.getMessage());
        }
        return "redirect:/team-member/tasks";
    }

    // =============================================
    // COMPLETE TASK
    // =============================================
    @PostMapping("/tasks/{taskId}/complete")
    public String completeTask(@PathVariable Long taskId, RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus("COMPLETED");
                task.setCompletedDate(LocalDateTime.now().toString());
                taskRepository.save(task);
                log.info("✅ Task {} completed by {}", task.getTitle(), email);
                redirectAttributes.addFlashAttribute("success", "✅ Task '" + task.getTitle() + "' marked as completed!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Task not found!");
            }
            
        } catch (Exception e) {
            log.error("Error completing task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to complete task: " + e.getMessage());
        }
        return "redirect:/team-member/tasks";
    }

    // =============================================
    // DELETE TASK
    // =============================================
    @PostMapping("/tasks/{taskId}/delete")
    public String deleteTask(@PathVariable Long taskId, RedirectAttributes redirectAttributes) {
        try {
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                String title = task.getTitle();
                taskRepository.deleteById(taskId);
                redirectAttributes.addFlashAttribute("success", "🗑️ Task '" + title + "' deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Task not found!");
            }
        } catch (Exception e) {
            log.error("Error deleting task: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Failed to delete task: " + e.getMessage());
        }
        return "redirect:/team-member/tasks";
    }

    // =============================================
    // TEAM
    // =============================================
    @GetMapping("/team")
    public String team(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        model.addAttribute("userEmail", email);
        return "team-member/team";
    }

    // =============================================
    // PROFILE
    // =============================================
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("userEmail", email);
        return "team-member/profile";
    }
}