package com.resumeai.auth.controller;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.resumeai.auth.dto.PaymentOrderRequest;
import com.resumeai.auth.dto.PaymentOrderResponse;
import com.resumeai.auth.dto.PaymentVerifyRequest;
import com.resumeai.auth.dto.AuthResponse;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import com.resumeai.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Exposes REST endpoints for payment workflows. */
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    /**
     * Creates a Razorpay order for premium upgrades.
     */
    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResponse> createOrder(@Valid @RequestBody PaymentOrderRequest request) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay keys are not configured");
        }

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject payload = new JSONObject();
            payload.put("amount", request.getAmount());
            payload.put("currency", request.getCurrency());
            payload.put("receipt", "resumeai_" + System.currentTimeMillis());
            payload.put("payment_capture", 1);

            Order order = client.orders.create(payload);

            return ResponseEntity.ok(PaymentOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .keyId(razorpayKeyId)
                    .plan(request.getPlan())
                    .build());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Razorpay order", ex);
        }
    }

    /**
     * Verifies payment signature and upgrades the user's subscription.
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyPayment(Authentication authentication,
                                                             @Valid @RequestBody PaymentVerifyRequest request) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // Validate Razorpay signature before updating subscription.
        String expectedSignature = hmacSha256(request.getOrderId() + "|" + request.getPaymentId(), razorpayKeySecret);
        if (!expectedSignature.equals(request.getSignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification failed");
        }

        // Upgrade to premium in DB
        authService.updateSubscription(authentication.getName(), "PREMIUM");

        // Generate a fresh JWT with updated subscriptionPlan=PREMIUM
        User updatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String freshToken = jwtUtil.generateToken(updatedUser);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(freshToken)
                .userId(updatedUser.getUserId())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .role(updatedUser.getRole().name())
                .subscriptionPlan(updatedUser.getSubscriptionPlan().name())
                .build());
    }

    /**
     * Computes HMAC SHA-256 signature for Razorpay verification.
     */
    private String hmacSha256(String data, String secret) {
        if (secret == null) {
            return "";
        }
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification error", ex);
        }
    }
}



