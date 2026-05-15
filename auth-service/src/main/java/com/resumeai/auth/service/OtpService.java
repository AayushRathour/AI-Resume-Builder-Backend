package com.resumeai.auth.service;

import com.resumeai.auth.dto.OtpResponse;
import com.resumeai.auth.entity.EmailVerification;
import com.resumeai.auth.entity.OtpPurpose;
import com.resumeai.auth.repository.EmailVerificationRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides supporting OTP operations for workflow execution. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailVerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_RESEND_PER_HOUR = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new OTP for the given email and purpose.
     * Returns the raw OTP (to be sent via email) and the verification metadata.
     */
    @Transactional
    public OtpGenerationResult generateOtp(String email, OtpPurpose purpose, Long userId) {
        email = email.trim().toLowerCase();

        // Rate limit: check resend cooldown
        Optional<EmailVerification> lastOtp = verificationRepository
                .findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(email, purpose);

        if (lastOtp.isPresent()) {
            LocalDateTime lastCreated = lastOtp.get().getCreatedAt();
            long secondsSinceLast = java.time.Duration.between(lastCreated, LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < RESEND_COOLDOWN_SECONDS) {
                int waitSeconds = (int) (RESEND_COOLDOWN_SECONDS - secondsSinceLast);
                log.warn("[OTP] Resend cooldown active for email={}, wait {} seconds", email, waitSeconds);
                return OtpGenerationResult.cooldown(waitSeconds);
            }
        }

        // Rate limit: max OTPs per hour
        long recentCount = verificationRepository.countRecentOtps(
                email, purpose, LocalDateTime.now().minusHours(1));
        if (recentCount >= MAX_RESEND_PER_HOUR) {
            log.warn("[OTP] Hourly rate limit exceeded for email={}", email);
            return OtpGenerationResult.rateLimited();
        }

        // Invalidate all previous active OTPs
        verificationRepository.invalidateAllActiveOtps(email, purpose);

        // Generate cryptographically secure 6-digit OTP
        String rawOtp = generateSecureOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        log.info("[OTP] Generated OTP for email={}, purpose={}", email, purpose);

        // Store hashed OTP
        EmailVerification verification = EmailVerification.builder()
                .userId(userId)
                .email(email)
                .otpCode(hashedOtp)
                .purpose(purpose)
                .isVerified(false)
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        verificationRepository.save(verification);
        log.info("[OTP] OTP stored successfully for email={}, verificationId={}", email, verification.getVerificationId());

        return OtpGenerationResult.success(rawOtp, OTP_EXPIRY_MINUTES * 60);
    }

    /**
     * Verifies an OTP for the given email and purpose.
     */
    @Transactional
    public OtpVerificationResult verifyOtp(String email, String rawOtp, OtpPurpose purpose) {
        email = email.trim().toLowerCase();

        Optional<EmailVerification> optVerification = verificationRepository
                .findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(email, purpose);

        if (optVerification.isEmpty()) {
            log.warn("[OTP] No active OTP found for email={}, purpose={}", email, purpose);
            return OtpVerificationResult.failure("No active OTP found. Please request a new one.");
        }

        EmailVerification verification = optVerification.get();

        // Check expiry
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[OTP] OTP expired for email={}", email);
            verification.setVerified(true); // Invalidate expired OTP
            verificationRepository.save(verification);
            return OtpVerificationResult.expired("OTP has expired. Please request a new one.");
        }

        // Check max attempts
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("[OTP] Max verification attempts exceeded for email={}", email);
            verification.setVerified(true); // Invalidate exhausted OTP
            verificationRepository.save(verification);
            return OtpVerificationResult.maxAttempts("Too many failed attempts. Please request a new OTP.");
        }

        // Increment attempts
        verification.setAttempts(verification.getAttempts() + 1);

        // Verify OTP hash
        if (!passwordEncoder.matches(rawOtp, verification.getOtpCode())) {
            verificationRepository.save(verification);
            int remaining = MAX_ATTEMPTS - verification.getAttempts();
            log.warn("[OTP] Invalid OTP for email={}, {} attempts remaining", email, remaining);
            return OtpVerificationResult.failure(
                    "Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        verification.setVerified(true);
        verificationRepository.save(verification);
        log.info("[OTP] OTP verified successfully for email={}", email);

        return OtpVerificationResult.success();
    }

    /**
     * Generates a cryptographically secure 6-digit OTP.
     */
    private String generateSecureOtp() {
        int otp = SECURE_RANDOM.nextInt(900000) + 100000; // 100000 - 999999
        return String.valueOf(otp);
    }


    public static class OtpGenerationResult {
        private final boolean success;
        private final String rawOtp;
        private final int expiresInSeconds;
        private final boolean cooldownActive;
        private final int waitSeconds;
        private final boolean rateLimited;

        private OtpGenerationResult(boolean success, String rawOtp, int expiresInSeconds,
                                     boolean cooldownActive, int waitSeconds, boolean rateLimited) {
            this.success = success;
            this.rawOtp = rawOtp;
            this.expiresInSeconds = expiresInSeconds;
            this.cooldownActive = cooldownActive;
            this.waitSeconds = waitSeconds;
            this.rateLimited = rateLimited;
        }

        public static OtpGenerationResult success(String rawOtp, int expiresInSeconds) {
            return new OtpGenerationResult(true, rawOtp, expiresInSeconds, false, 0, false);
        }

        public static OtpGenerationResult cooldown(int waitSeconds) {
            return new OtpGenerationResult(false, null, 0, true, waitSeconds, false);
        }

        public static OtpGenerationResult rateLimited() {
            return new OtpGenerationResult(false, null, 0, false, 0, true);
        }

        public boolean isSuccess() { return success; }
        public String getRawOtp() { return rawOtp; }
        public int getExpiresInSeconds() { return expiresInSeconds; }
        public boolean isCooldownActive() { return cooldownActive; }
        public int getWaitSeconds() { return waitSeconds; }
        public boolean isRateLimited() { return rateLimited; }
    }

    public static class OtpVerificationResult {
        private final boolean success;
        private final String message;
        private final boolean expired;
        private final boolean maxAttemptsReached;

        private OtpVerificationResult(boolean success, String message, boolean expired, boolean maxAttemptsReached) {
            this.success = success;
            this.message = message;
            this.expired = expired;
            this.maxAttemptsReached = maxAttemptsReached;
        }

        public static OtpVerificationResult success() {
            return new OtpVerificationResult(true, "OTP verified successfully", false, false);
        }

        public static OtpVerificationResult failure(String message) {
            return new OtpVerificationResult(false, message, false, false);
        }

        public static OtpVerificationResult expired(String message) {
            return new OtpVerificationResult(false, message, true, false);
        }

        public static OtpVerificationResult maxAttempts(String message) {
            return new OtpVerificationResult(false, message, false, true);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean isExpired() { return expired; }
        public boolean isMaxAttemptsReached() { return maxAttemptsReached; }
    }
}



