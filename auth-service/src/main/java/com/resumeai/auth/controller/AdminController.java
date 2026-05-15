package com.resumeai.auth.controller;

import com.resumeai.auth.client.AiServiceClient;
import com.resumeai.auth.client.ResumeServiceClient;
import com.resumeai.auth.client.TemplateServiceClient;
import com.resumeai.auth.dto.MessageResponse;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.service.AuthService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes REST endpoints for administration workflows. */

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AuthService authService;
    private final TemplateServiceClient templateServiceClient;
    private final AiServiceClient aiServiceClient;
    private final ResumeServiceClient resumeServiceClient;

    /**
     * Lists all users for admin management.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * Promotes a user to admin role.
     */
    @PutMapping("/users/{id}/upgrade")
    public ResponseEntity<UserProfileResponse> upgradeUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authService.upgradeUserToAdmin(userId));
    }

    /**
     * Suspends a user account.
     */
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<UserProfileResponse> suspendUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authService.suspendUser(userId));
    }

    /**
     * Restores a previously suspended or deleted account.
     */
    @PutMapping("/users/{id}/restore")
    public ResponseEntity<UserProfileResponse> restoreUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authService.restoreUser(userId));
    }

    /**
     * Updates subscription plan for a specific user.
     */
    @PutMapping("/users/{id}/subscription")
    public ResponseEntity<UserProfileResponse> updateSubscription(@PathVariable("id") Long userId,
                                                                  @RequestBody Map<String, String> payload) {
        String plan = payload.getOrDefault("plan", "FREE");
        return ResponseEntity.ok(authService.updateSubscriptionByUserId(userId, plan));
    }

    /**
     * Deletes a user account (soft delete).
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable("id") Long userId) {
        authService.deleteUserById(userId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    /**
     * Fetches template list from template-service.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        List<Map<String, Object>> templates = templateServiceClient.getTemplates();
        return ResponseEntity.ok(templates == null ? List.of() : templates);
    }

    /**
     * Creates a new template via template-service.
     */
    @PostMapping("/templates")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> payload) {
        Map<String, Object> created = templateServiceClient.createTemplate(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created == null ? Map.of() : created);
    }

    /**
     * Updates an existing template via template-service.
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable("id") Long id,
                                                               @RequestBody Map<String, Object> payload) {
        Map<String, Object> updated = templateServiceClient.updateTemplate(id, payload);
        return ResponseEntity.ok(updated == null ? Map.of() : updated);
    }

    /**
     * Provides high-level platform analytics for admin dashboards.
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        List<UserProfileResponse> users = authService.getAllUsers();
        List<Map<String, Object>> templates = fetchTemplatesSafely();

        long totalUsers = users.size();
        long activeUsers = users.stream().filter(UserProfileResponse::isActive).count();
        long premiumUsers = users.stream().filter(u -> "PREMIUM".equalsIgnoreCase(u.getSubscriptionPlan())).count();
        long adminUsers = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalUsers", totalUsers);
        analytics.put("activeUsers", activeUsers);
        analytics.put("premiumUsers", premiumUsers);
        analytics.put("adminUsers", adminUsers);
        analytics.put("templatesCount", templates.size());
        analytics.put("activeTemplates", templates.stream().filter(t -> Boolean.TRUE.equals(t.get("isActive"))).count());

        return ResponseEntity.ok(analytics);
    }

    /**
     * Provides summarized metrics for the admin dashboard.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        List<UserProfileResponse> users = authService.getAllUsers();
        long totalUsers = users.size();
        long premiumUsers = users.stream().filter(u -> "PREMIUM".equalsIgnoreCase(u.getSubscriptionPlan())).count();
        
        long totalTemplates = fetchTemplatesSafely().size();
        
        long totalResumes = fetchResumesSafely();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("users", totalUsers);
        dashboard.put("premiumUsers", premiumUsers);
        dashboard.put("templates", totalTemplates);
        dashboard.put("resumes", totalResumes);

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Aggregates AI usage statistics for admin monitoring.
     */
    @GetMapping("/ai-usage")
    public ResponseEntity<Map<String, Object>> getAiUsage() {
        List<UserProfileResponse> users = authService.getAllUsers();

        long usersWithUsage = 0;
        long totalRequests = 0;
        List<Map<String, Object>> perUser = new ArrayList<>();

        for (UserProfileResponse user : users) {
            try {
                Map<String, Object> history = aiServiceClient.getUserHistory(user.getUserId());
                int requests = aiServiceClient.extractHistoryCount(history);
                totalRequests += requests;
                if (requests > 0) {
                    usersWithUsage++;
                }

                Map<String, Object> userUsage = new HashMap<>();
                userUsage.put("userId", user.getUserId());
                userUsage.put("email", user.getEmail());
                userUsage.put("requests", requests);
                perUser.add(userUsage);
            } catch (Exception ex) {
                log.warn("Failed to fetch AI usage for user {}: {}", user.getUserId(), ex.getMessage());
            }
        }

        Map<String, Object> usage = new HashMap<>();
        usage.put("usersWithUsage", usersWithUsage);
        usage.put("totalRequests", totalRequests);
        usage.put("perUser", perUser);

        return ResponseEntity.ok(usage);
    }

    /**
     * Template-service lookup with fallback to empty results.
     */
    private List<Map<String, Object>> fetchTemplatesSafely() {
        try {
            List<Map<String, Object>> templates = templateServiceClient.getTemplates();
            return templates == null ? List.of() : templates;
        } catch (Exception ex) {
            log.warn("Failed to fetch templates for analytics: {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * Resume count lookup with fallback to zero.
     */
    private long fetchResumesSafely() {
        try {
            Long count = resumeServiceClient.countResumes(true);
            return count == null ? 0 : count;
        } catch (Exception ex) {
            log.warn("Failed to fetch resumes for analytics: {}", ex.getMessage());
            return 0;
        }
    }
}



