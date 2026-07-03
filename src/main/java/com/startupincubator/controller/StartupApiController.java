package com.startupincubator.controller;

import com.startupincubator.dto.ApiResponse;
import com.startupincubator.entity.Startup;
import com.startupincubator.enums.StartupStatus;
import com.startupincubator.repository.StartupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/startups")
@RequiredArgsConstructor
@Slf4j
public class StartupApiController {

    private final StartupRepository startupRepository;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createStartup(@RequestBody Startup startup) {
        try {
            startup.setCreatedAt(LocalDateTime.now());
            startup.setUpdatedAt(LocalDateTime.now());
            if (startup.getStatus() == null) {
                startup.setStatus(StartupStatus.PENDING);
            }
            Startup saved = startupRepository.save(startup);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Startup created successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating startup: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating startup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStartup(@PathVariable Long id, @RequestBody Startup startup) {
        try {
            Startup existing = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            existing.setName(startup.getName());
            existing.setCategory(startup.getCategory());
            existing.setDescription(startup.getDescription());
            existing.setStatus(startup.getStatus());
            existing.setUpdatedAt(LocalDateTime.now());
            
            Startup updated = startupRepository.save(existing);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Startup updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating startup: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating startup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteStartup(@PathVariable Long id) {
        try {
            startupRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Startup deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting startup: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting startup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveStartup(@PathVariable Long id) {
        try {
            Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            startup.setStatus(StartupStatus.APPROVED);
            startup.setIsApproved(true);
            startup.setUpdatedAt(LocalDateTime.now());
            startupRepository.save(startup);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Startup approved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving startup: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error approving startup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectStartup(@PathVariable Long id) {
        try {
            Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            startup.setStatus(StartupStatus.REJECTED);
            startup.setIsApproved(false);
            startup.setUpdatedAt(LocalDateTime.now());
            startupRepository.save(startup);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Startup rejected successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting startup: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error rejecting startup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Startup>> activateStartup(@PathVariable Long id) {
        try {
            Startup startup = startupRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Startup not found"));
            
            startup.setStatus(StartupStatus.ACTIVE);
            startup.setUpdatedAt(LocalDateTime.now());
            
            Startup updated = startupRepository.save(startup);
            
            return ResponseEntity.ok(ApiResponse.success("Startup activated successfully", updated));
            
        } catch (Exception e) {
            log.error("Error activating startup: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to activate startup: " + e.getMessage()));
        }
    }
    
}