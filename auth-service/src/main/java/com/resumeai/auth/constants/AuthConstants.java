package com.resumeai.auth.constants;

/**
 * Centralized auth constants shared across service layers.
 */
public final class AuthConstants {

    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String ADMIN_ACCESS_REQUIRED = "Admin access required";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";

    public static final String ACCOUNT_SUSPENDED = "Account is suspended";
    public static final String ACCOUNT_DELETED = "Account is deleted";
    public static final String USER_NOT_FOUND = "User not found";

    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String EMAIL_AND_PASSWORD_REQUIRED = "Email and password are required";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_AND_OTP_REQUIRED = "Email and OTP are required";

    public static final String OTP_GENERATED_SUCCESSFULLY = "OTP generated successfully";
    public static final String OTP_COOLDOWN_MESSAGE = "Please wait %d seconds before requesting a new OTP";
    public static final String OTP_RATE_LIMITED = "Too many OTP requests. Please try again later.";
    public static final String EMAIL_VERIFIED_SUCCESSFULLY = "Email verified successfully";

    public static final String ADMIN_CANNOT_BE_SUSPENDED = "Admin account cannot be suspended";
    public static final String ADMIN_CANNOT_BE_DELETED = "Admin account cannot be deleted";
    public static final String DELETED_ADMIN_CANNOT_BE_RESTORED = "Deleted admin account cannot be auto-restored";
    public static final String DELETED_ACCOUNT_CANNOT_BE_SUSPENDED = "Deleted account cannot be suspended";

    public static final String PASSWORD_UPDATED_SUCCESSFULLY = "Password updated successfully";
    public static final String PROFILE_UPDATED_SUCCESSFULLY = "Profile updated successfully";
    public static final String ACCOUNT_DEACTIVATED_SUCCESSFULLY = "Account deactivated successfully";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";

    public static final String INVALID_SUBSCRIPTION_PLAN = "Invalid subscription plan. Use FREE or PREMIUM";

    public static final String ACCOUNT_SUSPENDED_CONTACT_SUPPORT = "Account is suspended. Contact support.";
    public static final String ACCOUNT_DELETED_CONTACT_ADMIN = "Account is deleted. Contact admin.";

    private AuthConstants() {
    }
}