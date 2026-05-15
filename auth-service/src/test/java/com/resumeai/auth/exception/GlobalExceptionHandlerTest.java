package com.resumeai.auth.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.method.annotation.ModelFactory;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void handleInvalidCredentials() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid");
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidCredentials(ex, request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid", response.getBody().getMessage());
    }

    @Test
    void handleUserNotFound() {
        UserNotFoundException ex = new UserNotFoundException("Not found");
        ResponseEntity<ApiErrorResponse> response = handler.handleUserNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handleAccountAccessExceptions() {
        AccountSuspendedException ex1 = new AccountSuspendedException("Suspended");
        ResponseEntity<ApiErrorResponse> response1 = handler.handleAccountAccessExceptions(ex1, request);
        assertEquals(HttpStatus.FORBIDDEN, response1.getStatusCode());
        
        AccountDeletedException ex2 = new AccountDeletedException("Deleted");
        ResponseEntity<ApiErrorResponse> response2 = handler.handleAccountAccessExceptions(ex2, request);
        assertEquals(HttpStatus.FORBIDDEN, response2.getStatusCode());
    }

    @Test
    void handleOtpExceptions() {
        InvalidOtpException ex1 = new InvalidOtpException("Invalid OTP");
        ResponseEntity<ApiErrorResponse> response1 = handler.handleOtpExceptions(ex1, request);
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        
        OtpExpiredException ex2 = new OtpExpiredException("Expired OTP");
        ResponseEntity<ApiErrorResponse> response2 = handler.handleOtpExceptions(ex2, request);
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
    }

    @Test
    void handleEmailNotVerified() {
        EmailNotVerifiedException ex = new EmailNotVerifiedException("Not verified");
        ResponseEntity<ApiErrorResponse> response = handler.handleEmailNotVerified(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleUnauthorizedAccess() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("Unauthorized");
        ResponseEntity<ApiErrorResponse> response = handler.handleUnauthorizedAccess(ex, request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleRuntimeException() {
        RuntimeException ex = new RuntimeException("Some generic error");
        ResponseEntity<ApiErrorResponse> response = handler.handleRuntimeException(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void resolveRuntimeStatus_allCases() {
        ResponseEntity<ApiErrorResponse> notFound = handler.handleRuntimeException(new RuntimeException("User not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        
        ResponseEntity<ApiErrorResponse> suspended = handler.handleRuntimeException(new RuntimeException("Account is suspended"), request);
        assertEquals(HttpStatus.FORBIDDEN, suspended.getStatusCode());
        
        ResponseEntity<ApiErrorResponse> deleted = handler.handleRuntimeException(new RuntimeException("Account is deleted"), request);
        assertEquals(HttpStatus.FORBIDDEN, deleted.getStatusCode());
        
        ResponseEntity<ApiErrorResponse> disabled = handler.handleRuntimeException(new RuntimeException("Account is disabled"), request);
        assertEquals(HttpStatus.FORBIDDEN, disabled.getStatusCode());
    }

    @Test
    void emailVerificationException_constructor_shouldRetainMessage() {
        EmailVerificationException ex = new EmailVerificationException("verification failed");
        assertEquals("verification failed", ex.getMessage());
    }

    @Test
    void handleEmailVerification() {
        EmailVerificationException ex = new EmailVerificationException("Email verification failed");
        ResponseEntity<ApiErrorResponse> response = handler.handleEmailVerification(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Email verification failed", response.getBody().getMessage());
    }

    @Test
    void handleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatusException(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad request", response.getBody().getMessage());
    }

    @Test
    void handleValidationException_shouldJoinMessages() throws Exception {
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email is required, Password is required", response.getBody().getMessage());
    }
}
