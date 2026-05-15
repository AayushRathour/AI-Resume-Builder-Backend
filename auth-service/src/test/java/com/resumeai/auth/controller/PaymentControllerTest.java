package com.resumeai.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.dto.PaymentOrderRequest;
import com.resumeai.auth.dto.UserProfileResponse;
import com.resumeai.auth.dto.PaymentVerifyRequest;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.SubscriptionPlan;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import com.resumeai.auth.security.JwtUtil;
import com.resumeai.auth.service.AuthService;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"razorpay.key-id=", "razorpay.key-secret=testsecret"})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void createOrder_missingKeys_shouldReturnBadRequest() throws Exception {
        // when keys are empty via TestPropertySource key-id is empty -> should return BAD_REQUEST
        PaymentOrderRequest req = new PaymentOrderRequest();
        req.setAmount(1000L);
        req.setCurrency("INR");
        req.setPlan("PREMIUM");

        mockMvc.perform(post("/payment/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyPayment_success() throws Exception {
        // prepare authentication principal email
        String email = "user@test.com";

        User user = User.builder().userId(1L).email(email).fullName("User").role(Role.USER)
                .subscriptionPlan(SubscriptionPlan.FREE).isActive(true).isDeleted(false).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("fresh-token");
        when(authService.updateSubscription(anyString(), anyString()))
            .thenReturn(UserProfileResponse.builder().subscriptionPlan(SubscriptionPlan.PREMIUM.name()).build());

        String orderId = "order_123";
        String paymentId = "pay_456";
        String secret = "testsecret";
        String signature = computeHmac(orderId + "|" + paymentId, secret);

        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setOrderId(orderId);
        req.setPaymentId(paymentId);
        req.setSignature(signature);

        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, "n/a", java.util.Collections.emptyList());

        mockMvc.perform(post("/payment/verify").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void verifyPayment_missingAuthentication_shouldReturnUnauthorized() throws Exception {
        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setOrderId("order_123");
        req.setPaymentId("pay_456");
        req.setSignature("signature");

        mockMvc.perform(post("/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyPayment_invalidSignature_shouldReturnBadRequest() throws Exception {
        String email = "user@test.com";
        User user = User.builder().userId(1L).email(email).fullName("User").role(Role.USER)
                .subscriptionPlan(SubscriptionPlan.FREE).isActive(true).isDeleted(false).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        PaymentVerifyRequest req = new PaymentVerifyRequest();
        req.setOrderId("order_123");
        req.setPaymentId("pay_456");
        req.setSignature("wrong-signature");

        org.springframework.security.core.Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, "n/a", java.util.Collections.emptyList());

        mockMvc.perform(post("/payment/verify").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    private String computeHmac(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
