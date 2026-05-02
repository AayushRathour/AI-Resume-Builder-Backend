package com.resumeai.auth.controller;

import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.ChangePasswordRequest;
import com.resumeai.auth.dto.LoginRequest;
import com.resumeai.auth.dto.MessageResponse;
import com.resumeai.auth.dto.RegisterRequest;
import com.resumeai.auth.dto.SubscriptionUpdateRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @RequestBody RegisterRequest request) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(Authentication authentication,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        authService.changePassword(authentication.getName(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password updated successfully"));
    }

    @PutMapping("/subscription")
    public ResponseEntity<UserProfileResponse> updateSubscription(Authentication authentication,
                                                                  @Valid @RequestBody SubscriptionUpdateRequest request) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.updateSubscription(authentication.getName(), request.getPlan()));
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<MessageResponse> deactivateAccount(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        authService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Account deactivated successfully"));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<java.util.List<UserProfileResponse>> getAllUsers(Authentication authentication) {
        ensureAdmin(authentication);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(Authentication authentication,
                                                      @PathVariable Long userId) {
        ensureAdmin(authentication);
        authService.deleteUserById(userId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    @PutMapping("/admin/users/{userId}/subscription")
    public ResponseEntity<UserProfileResponse> updateUserSubscription(Authentication authentication,
                                                                      @PathVariable Long userId,
                                                                      @Valid @RequestBody SubscriptionUpdateRequest request) {
        ensureAdmin(authentication);
        return ResponseEntity.ok(authService.updateSubscriptionByUserId(userId, request.getPlan()));
    }

    private void ensureAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
