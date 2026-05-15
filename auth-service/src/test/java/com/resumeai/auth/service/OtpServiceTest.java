package com.resumeai.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.resumeai.auth.entity.EmailVerification;
import com.resumeai.auth.entity.OtpPurpose;
import com.resumeai.auth.repository.EmailVerificationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    private EmailVerification verification;

    @BeforeEach
    void setUp() {
        verification = EmailVerification.builder()
                .verificationId(1L)
                .email("test@test.com")
                .purpose(OtpPurpose.LOGIN)
                .otpCode("hashed_otp")
                .isVerified(false)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    @Test
    void generateOtp_success() {
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.empty());
        when(verificationRepository.countRecentOtps(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");

        OtpService.OtpGenerationResult result = otpService.generateOtp("test@test.com", OtpPurpose.LOGIN, 1L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRawOtp());
        assertEquals(300, result.getExpiresInSeconds());

        verify(verificationRepository).invalidateAllActiveOtps("test@test.com", OtpPurpose.LOGIN);
        verify(verificationRepository).save(any(EmailVerification.class));
    }

    @Test
    void generateOtp_cooldown() {
        verification.setCreatedAt(LocalDateTime.now().minusSeconds(10)); // 10 seconds ago
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.of(verification));

        OtpService.OtpGenerationResult result = otpService.generateOtp("test@test.com", OtpPurpose.LOGIN, 1L);

        assertFalse(result.isSuccess());
        assertTrue(result.isCooldownActive());
        assertTrue(result.getWaitSeconds() > 0);
    }

    @Test
    void generateOtp_rateLimited() {
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.empty());
        when(verificationRepository.countRecentOtps(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
                .thenReturn(10L);

        OtpService.OtpGenerationResult result = otpService.generateOtp("test@test.com", OtpPurpose.LOGIN, 1L);

        assertFalse(result.isSuccess());
        assertTrue(result.isRateLimited());
    }

    @Test
    void verifyOtp_success() {
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);

        OtpService.OtpVerificationResult result = otpService.verifyOtp("test@test.com", "123456", OtpPurpose.LOGIN);

        assertTrue(result.isSuccess());
        verify(verificationRepository).save(verification);
        assertTrue(verification.isVerified());
    }

    @Test
    void verifyOtp_noActiveOtp() {
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.empty());

        OtpService.OtpVerificationResult result = otpService.verifyOtp("test@test.com", "123456", OtpPurpose.LOGIN);

        assertFalse(result.isSuccess());
        assertEquals("No active OTP found. Please request a new one.", result.getMessage());
    }

    @Test
    void verifyOtp_expired() {
        verification.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.of(verification));

        OtpService.OtpVerificationResult result = otpService.verifyOtp("test@test.com", "123456", OtpPurpose.LOGIN);

        assertFalse(result.isSuccess());
        assertTrue(result.isExpired());
        assertTrue(verification.isVerified()); // Invalidated
    }

    @Test
    void verifyOtp_maxAttempts() {
        verification.setAttempts(5);
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.of(verification));

        OtpService.OtpVerificationResult result = otpService.verifyOtp("test@test.com", "123456", OtpPurpose.LOGIN);

        assertFalse(result.isSuccess());
        assertTrue(result.isMaxAttemptsReached());
        assertTrue(verification.isVerified()); // Invalidated
    }

    @Test
    void verifyOtp_invalidOtp() {
        when(verificationRepository.findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
                anyString(), any(OtpPurpose.class))).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(false);

        OtpService.OtpVerificationResult result = otpService.verifyOtp("test@test.com", "123456", OtpPurpose.LOGIN);

        assertFalse(result.isSuccess());
        assertEquals(1, verification.getAttempts());
        verify(verificationRepository).save(verification);
    }
}
