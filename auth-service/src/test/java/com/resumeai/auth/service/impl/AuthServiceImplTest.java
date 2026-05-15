package com.resumeai.auth.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.*;
import com.resumeai.auth.exception.*;
import com.resumeai.auth.repository.EmailVerificationRepository;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import com.resumeai.auth.service.OtpService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private OtpService otpService;

    @InjectMocks private AuthServiceImpl authService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L).email("user@test.com").fullName("Test User")
                .password("hashed-password").role(Role.USER).provider(Provider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE).isActive(true).isDeleted(false)
                .build();
        adminUser = User.builder()
                .userId(2L).email("admin@test.com").role(Role.ADMIN).isActive(true).isDeleted(false)
                .build();
    }

    @Test
    void getProfile_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        UserProfileResponse resp = authService.getProfile("user@test.com");
        assertNotNull(resp);
        assertEquals("user@test.com", resp.getEmail());
    }

    @Test
    void getProfile_userNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> authService.getProfile("notfound@test.com"));
    }

    @Test
    void register_newUser_success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.com")
                .password("password")
                .fullName("New User")
                .build();

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp("new@test.com", OtpPurpose.REGISTER, null))
                .thenReturn(OtpService.OtpGenerationResult.success("123456", 300));

        AuthResponse response = authService.register(request);

        assertTrue(response.isRequiresOtp());
        assertEquals("new@test.com", response.getOtpEmail());
        assertEquals("REGISTER", response.getOtpPurpose());
        verify(userRepository).save(any(User.class));
    }

        @Test
        void register_newUser_otpCooldown_shouldFail() {
        RegisterRequest request = RegisterRequest.builder()
            .email("cooldown@test.com")
            .password("password")
            .fullName("Cooldown User")
            .build();

        when(userRepository.findByEmail("cooldown@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp("cooldown@test.com", OtpPurpose.REGISTER, null))
            .thenReturn(OtpService.OtpGenerationResult.cooldown(12));

        assertThrows(InvalidOtpException.class, () -> authService.register(request));
        }

        @Test
        void register_newUser_rateLimited_shouldFail() {
        RegisterRequest request = RegisterRequest.builder()
            .email("ratelimit@test.com")
            .password("password")
            .fullName("Rate Limit User")
            .build();

        when(userRepository.findByEmail("ratelimit@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp("ratelimit@test.com", OtpPurpose.REGISTER, null))
            .thenReturn(OtpService.OtpGenerationResult.rateLimited());

        assertThrows(InvalidOtpException.class, () -> authService.register(request));
        }

    @Test
    void register_existingUser_withValidPassword_shouldReturnLoginOtp() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(otpService.generateOtp("user@test.com", OtpPurpose.LOGIN, 1L))
                .thenReturn(OtpService.OtpGenerationResult.success("123456", 300));

        AuthResponse response = authService.register(RegisterRequest.builder()
                .email("user@test.com")
                .password("password")
                .build());

        assertTrue(response.isRequiresOtp());
        assertEquals("LOGIN", response.getOtpPurpose());
    }

        @Test
        void register_existingUserWithBlankPassword_shouldSetPasswordAndName() {
        User existing = User.builder()
            .userId(3L)
            .email("blank@test.com")
            .fullName(null)
            .password("")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .subscriptionPlan(SubscriptionPlan.FREE)
            .isActive(true)
            .isDeleted(false)
            .build();

        when(userRepository.findByEmail("blank@test.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp("blank@test.com", OtpPurpose.LOGIN, 3L))
            .thenReturn(OtpService.OtpGenerationResult.success("123456", 300));

        AuthResponse response = authService.register(RegisterRequest.builder()
            .email("blank@test.com")
            .password("password")
            .fullName("Blank User")
            .build());

        assertTrue(response.isRequiresOtp());
        assertEquals("Blank User", existing.getFullName());
        assertEquals("encoded-password", existing.getPassword());
        }

    @Test
    void register_existingDeletedUser_shouldFail() {
        testUser.setDeleted(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        RegisterRequest req = RegisterRequest.builder()
                .email("user@test.com")
                .password("password")
                .build();
        assertThrows(AccountDeletedException.class, () -> authService.register(req));
    }

    @Test
    void register_nullRequest_shouldFail() {
        assertThrows(InvalidCredentialsException.class, () -> authService.register(null));
    }

    @Test
    void login_invalidPassword_shouldFail() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("bad-password", "hashed-password")).thenReturn(false);

        LoginRequest req = LoginRequest.builder().email("user@test.com").password("bad-password").build();
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_suspendedUser_shouldFail() {
        testUser.setActive(false);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        LoginRequest req = LoginRequest.builder().email("user@test.com").password("password").build();
        assertThrows(AccountSuspendedException.class, () -> authService.login(req));
    }

    @Test
    void login_nullRequest_shouldFail() {
        assertThrows(InvalidCredentialsException.class, () -> authService.login(null));
    }

    @Test
    void sendOtp_deletedAccount_shouldReturnFailureResponse() {
        testUser.setDeleted(true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        OtpResponse response = authService.sendOtp("user@test.com", "LOGIN");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("deleted"));
    }

    @Test
    void sendOtp_suspendedAccount_shouldReturnFailureResponse() {
        testUser.setActive(false);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        OtpResponse response = authService.sendOtp("user@test.com", "LOGIN");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("suspended"));
    }

    @Test
    void sendOtp_cooldown_shouldReturnFailureResponse() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp("user@test.com", OtpPurpose.LOGIN, 1L))
                .thenReturn(OtpService.OtpGenerationResult.cooldown(15));

        OtpResponse response = authService.sendOtp("user@test.com", "LOGIN");

        assertFalse(response.isSuccess());
        assertEquals(15, response.getResendCooldownSeconds());
    }

    @Test
    void sendOtp_rateLimited_shouldReturnFailureResponse() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp("user@test.com", OtpPurpose.LOGIN, 1L))
                .thenReturn(OtpService.OtpGenerationResult.rateLimited());

        OtpResponse response = authService.sendOtp("user@test.com", "LOGIN");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Too many OTP requests"));
    }

    @Test
    void sendOtp_nullPurpose_shouldDefaultToLogin() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp("user@test.com", OtpPurpose.LOGIN, 1L))
                .thenReturn(OtpService.OtpGenerationResult.success("123456", 300));

        OtpResponse response = authService.sendOtp("user@test.com", null);

        assertTrue(response.isSuccess());
    }

    @Test
    void verifyOtp_expired_shouldReturnFailureResponse() {
        when(otpService.verifyOtp("user@test.com", "123456", OtpPurpose.LOGIN))
                .thenReturn(OtpService.OtpVerificationResult.expired("OTP has expired. Please request a new one."));

        OtpResponse response = authService.verifyOtp("user@test.com", "123456", "LOGIN");

        assertFalse(response.isSuccess());
        assertEquals("OTP has expired. Please request a new one.", response.getMessage());
    }

    @Test
    void verifyOtp_userNotFound_shouldFail() {
        when(otpService.verifyOtp("user@test.com", "123456", OtpPurpose.LOGIN))
                .thenReturn(OtpService.OtpVerificationResult.success());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.verifyOtp("user@test.com", "123456", "LOGIN"));
    }

    @Test
    void verifyOtp_nullPurpose_shouldDefaultToLogin() {
        when(otpService.verifyOtp("user@test.com", "123456", OtpPurpose.LOGIN))
                .thenReturn(OtpService.OtpVerificationResult.success());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("token123");

        OtpResponse response = authService.verifyOtp("user@test.com", "123456", null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getAuthResponse());
    }

    @Test
    void updateSubscriptionByUserId_notFound_shouldFail() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.updateSubscriptionByUserId(99L, "PREMIUM"));
    }

    @Test
    void suspendUser_deletedAccount_shouldFail() {
        testUser.setDeleted(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(AccountDeletedException.class, () -> authService.suspendUser(1L));
    }

    @Test
    void restoreUser_deletedAdmin_shouldFail() {
        adminUser.setDeleted(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalArgumentException.class, () -> authService.restoreUser(2L));
    }

    @Test
    void deleteUserById_adminShouldFail() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalArgumentException.class, () -> authService.deleteUserById(2L));
    }

    @Test
    void updateSubscription_invalidPlan_shouldFail() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> authService.updateSubscription("user@test.com", "GOLD"));
    }

    @Test
    void updateProfile_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        RegisterRequest req = RegisterRequest.builder().fullName("Updated Name").phone("1234567890").build();
        UserProfileResponse resp = authService.updateProfile("user@test.com", req);
        
        assertNotNull(resp);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new-hashed-password");

        authService.changePassword("user@test.com", "oldPass", "newPass");
        verify(userRepository).save(testUser);
        assertEquals("new-hashed-password", testUser.getPassword());
    }

    @Test
    void changePassword_invalidOldPassword() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPass", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword("user@test.com", "wrongOldPass", "newPass"));
    }

    @Test
    void deactivateAccount_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        
        authService.deactivateAccount("user@test.com");
        
        verify(userRepository).save(testUser);
        assertFalse(testUser.isActive());
        verify(emailVerificationRepository, atLeastOnce()).invalidateAllActiveOtps(eq("user@test.com"), any());
    }

    @Test
    void updateSubscription_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserProfileResponse resp = authService.updateSubscription("user@test.com", "PREMIUM");
        assertEquals(SubscriptionPlan.PREMIUM.name(), resp.getSubscriptionPlan());
    }

    @Test
    void updateSubscriptionByUserId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserProfileResponse resp = authService.updateSubscriptionByUserId(1L, "PREMIUM");
        assertEquals(SubscriptionPlan.PREMIUM.name(), resp.getSubscriptionPlan());
    }

    @Test
    void upgradeUserToAdmin_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserProfileResponse resp = authService.upgradeUserToAdmin(1L);
        assertEquals(Role.ADMIN.name(), resp.getRole());
    }

    @Test
    void suspendUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserProfileResponse resp = authService.suspendUser(1L);
        assertFalse(resp.isActive());
    }

    @Test
    void suspendUser_adminNotAllowed() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        assertThrows(IllegalArgumentException.class, () -> authService.suspendUser(2L));
    }

    @Test
    void restoreUser_success() {
        testUser.setDeleted(true);
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserProfileResponse resp = authService.restoreUser(1L);
        assertTrue(resp.isActive());
        assertFalse(resp.isDeleted());
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<UserProfileResponse> users = authService.getAllUsers();
        assertEquals(1, users.size());
    }

    @Test
    void deleteUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        authService.deleteUserById(1L);
        
        verify(userRepository).save(testUser);
        assertTrue(testUser.isDeleted());
        assertFalse(testUser.isActive());
    }

    @Test
    void sendOtp_success() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        OtpService.OtpGenerationResult res = OtpService.OtpGenerationResult.success("123456", 300);
        when(otpService.generateOtp("user@test.com", OtpPurpose.LOGIN, 1L)).thenReturn(res);

        OtpResponse resp = authService.sendOtp("user@test.com", "LOGIN");
        assertTrue(resp.isSuccess());
    }

    @Test
    void verifyOtp_success() {
        when(otpService.verifyOtp("user@test.com", "123456", OtpPurpose.LOGIN)).thenReturn(OtpService.OtpVerificationResult.success());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("token123");

        OtpResponse resp = authService.verifyOtp("user@test.com", "123456", "LOGIN");
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getAuthResponse());
        assertEquals("token123", resp.getAuthResponse().getToken());
    }
}
