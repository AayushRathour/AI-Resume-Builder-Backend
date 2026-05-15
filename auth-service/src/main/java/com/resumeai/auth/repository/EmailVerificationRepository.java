package com.resumeai.auth.repository;

import com.resumeai.auth.entity.EmailVerification;
import com.resumeai.auth.entity.OtpPurpose;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for persistence and query operations in this domain. */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * Find the latest non-verified OTP for a given email and purpose.
     */
    Optional<EmailVerification> findTopByEmailAndPurposeAndIsVerifiedFalseOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);

    /**
     * Invalidate all active (non-verified, non-expired) OTPs for an email and purpose.
     */
    @Modifying
    @Query("UPDATE EmailVerification e SET e.isVerified = true WHERE e.email = :email AND e.purpose = :purpose AND e.isVerified = false")
    void invalidateAllActiveOtps(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    /**
     * Count OTPs created in a given time window for rate-limiting.
     */
    @Query("SELECT COUNT(e) FROM EmailVerification e WHERE e.email = :email AND e.purpose = :purpose AND e.createdAt > :since")
    long countRecentOtps(@Param("email") String email, @Param("purpose") OtpPurpose purpose, @Param("since") LocalDateTime since);

    /**
     * Cleanup expired verifications (optional maintenance).
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now AND e.isVerified = false")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

