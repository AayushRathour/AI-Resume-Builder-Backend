package com.resumeai.auth.constants;

/** Centralized authentication and account-management message constants for auth-service. */
public class AuthMessages {
    public static final String AUTHENTICATION_REQUIRED = AuthConstants.AUTHENTICATION_REQUIRED;
    public static final String ADMIN_ACCESS_REQUIRED = AuthConstants.ADMIN_ACCESS_REQUIRED;
    public static final String UNAUTHORIZED_ACCESS = AuthConstants.UNAUTHORIZED_ACCESS;
    public static final String ACCOUNT_SUSPENDED = AuthConstants.ACCOUNT_SUSPENDED;
    public static final String ACCOUNT_DELETED = AuthConstants.ACCOUNT_DELETED;
    public static final String USER_NOT_FOUND = AuthConstants.USER_NOT_FOUND;
    public static final String INVALID_CREDENTIALS = AuthConstants.INVALID_CREDENTIALS;
    public static final String EMAIL_AND_PASSWORD_REQUIRED = AuthConstants.EMAIL_AND_PASSWORD_REQUIRED;
    public static final String EMAIL_REQUIRED = AuthConstants.EMAIL_REQUIRED;
    public static final String EMAIL_AND_OTP_REQUIRED = AuthConstants.EMAIL_AND_OTP_REQUIRED;
    public static final String OTP_GENERATED_SUCCESSFULLY = AuthConstants.OTP_GENERATED_SUCCESSFULLY;
    public static final String OTP_COOLDOWN_MESSAGE = AuthConstants.OTP_COOLDOWN_MESSAGE;
    public static final String OTP_RATE_LIMITED = AuthConstants.OTP_RATE_LIMITED;
    public static final String EMAIL_VERIFIED_SUCCESSFULLY = AuthConstants.EMAIL_VERIFIED_SUCCESSFULLY;
    public static final String ADMIN_CANNOT_BE_SUSPENDED = AuthConstants.ADMIN_CANNOT_BE_SUSPENDED;
    public static final String ADMIN_CANNOT_BE_DELETED = AuthConstants.ADMIN_CANNOT_BE_DELETED;
    public static final String DELETED_ADMIN_CANNOT_BE_RESTORED = AuthConstants.DELETED_ADMIN_CANNOT_BE_RESTORED;
    public static final String DELETED_ACCOUNT_CANNOT_BE_SUSPENDED = AuthConstants.DELETED_ACCOUNT_CANNOT_BE_SUSPENDED;
    public static final String PASSWORD_UPDATED_SUCCESSFULLY = AuthConstants.PASSWORD_UPDATED_SUCCESSFULLY;
    public static final String PROFILE_UPDATED_SUCCESSFULLY = AuthConstants.PROFILE_UPDATED_SUCCESSFULLY;
    public static final String ACCOUNT_DEACTIVATED_SUCCESSFULLY = AuthConstants.ACCOUNT_DEACTIVATED_SUCCESSFULLY;
    public static final String USER_DELETED_SUCCESSFULLY = AuthConstants.USER_DELETED_SUCCESSFULLY;
    public static final String INVALID_SUBSCRIPTION_PLAN = AuthConstants.INVALID_SUBSCRIPTION_PLAN;
    public static final String ACCOUNT_SUSPENDED_CONTACT_SUPPORT = AuthConstants.ACCOUNT_SUSPENDED_CONTACT_SUPPORT;
    public static final String ACCOUNT_DELETED_CONTACT_ADMIN = AuthConstants.ACCOUNT_DELETED_CONTACT_ADMIN;

    private AuthMessages() {
    }
}
