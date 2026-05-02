package com.resumeai.auth.controller;

import com.resumeai.auth.dto.MessageResponse;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.service.AuthService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AuthService authService;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.template-service.base-url}")
    private String templateServiceBaseUrl;

    @Value("${app.ai-service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${app.resume-service.base-url}")
    private String resumeServiceBaseUrl;

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/users/{id}/upgrade")
    public ResponseEntity<UserProfileResponse> upgradeUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authService.upgradeUserToAdmin(userId));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<UserProfileResponse> suspendUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(authService.suspendUser(userId));
    }

    @PutMapping("/users/{id}/subscription")
    public ResponseEntity<UserProfileResponse> updateSubscription(@PathVariable("id") Long userId,
                                                                  @RequestBody Map<String, String> payload) {
        String plan = payload.getOrDefault("plan", "FREE");
        return ResponseEntity.ok(authService.updateSubscriptionByUserId(userId, plan));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable("id") Long userId) {
        authService.deleteUserById(userId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        List<Map<String, Object>> templates = templateClient().get()
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(templates == null ? List.of() : templates);
    }

    @PostMapping("/templates")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> payload) {
        Map<String, Object> created = templateClient().post()
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.status(HttpStatus.CREATED).body(created == null ? Map.of() : created);
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(@PathVariable("id") Long id,
                                                               @RequestBody Map<String, Object> payload) {
        Map<String, Object> updated = templateClient().put()
                .uri("/{id}", id)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return ResponseEntity.ok(updated == null ? Map.of() : updated);
    }

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

    @GetMapping("/ai-usage")
    public ResponseEntity<Map<String, Object>> getAiUsage() {
        List<UserProfileResponse> users = authService.getAllUsers();

        long usersWithUsage = 0;
        long totalRequests = 0;
        List<Map<String, Object>> perUser = new ArrayList<>();

        RestClient aiClient = aiClient();
        for (UserProfileResponse user : users) {
            try {
                List<Map<String, Object>> history = aiClient.get()
                        .uri("/history/{userId}", user.getUserId())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                int requests = history == null ? 0 : history.size();
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

    private List<Map<String, Object>> fetchTemplatesSafely() {
        try {
            List<Map<String, Object>> templates = templateClient().get()
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return templates == null ? List.of() : templates;
        } catch (Exception ex) {
            log.warn("Failed to fetch templates for analytics: {}", ex.getMessage());
            return List.of();
        }
    }

    private long fetchResumesSafely() {
        try {
            // Assume resume-service /api/resumes/count returns a Long or /api/resumes returns list of resumes. 
            // We fetch the count if possible, but for safety against unknown APIs, we might just get all or hit a stats endpoint.
            // According to standard CRUD, GET / returns a list.
            List<Map<String, Object>> resumes = restClientBuilder.baseUrl(resumeServiceBaseUrl).build().get()
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return resumes == null ? 0 : resumes.size();
        } catch (Exception ex) {
            log.warn("Failed to fetch resumes for analytics: {}", ex.getMessage());
            return 0; // Return 0 if there's no such endpoint
        }
    }

    private RestClient templateClient() {
        return restClientBuilder.baseUrl(templateServiceBaseUrl).build();
    }

    private RestClient aiClient() {
        return restClientBuilder.baseUrl(aiServiceBaseUrl).build();
    }
}
