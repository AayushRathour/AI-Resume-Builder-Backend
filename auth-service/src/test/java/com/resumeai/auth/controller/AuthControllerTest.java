package com.resumeai.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.auth.dto.*;
import com.resumeai.auth.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnAuthResponse() throws Exception {
        RegisterRequest req = RegisterRequest.builder().email("test@test.com").password("password").fullName("Test User").build();
        AuthResponse resp = AuthResponse.builder().requiresOtp(true).otpEmail("test@test.com").build();
        when(authService.register(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresOtp").value(true));
    }

    @Test
    void login_shouldReturnAuthResponse() throws Exception {
        LoginRequest req = LoginRequest.builder().email("test@test.com").password("password").build();
        AuthResponse resp = AuthResponse.builder().requiresOtp(true).otpEmail("test@test.com").build();
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresOtp").value(true));
    }

    @Test
    void sendOtp_shouldReturnOtpResponse() throws Exception {
        OtpResponse resp = OtpResponse.builder().success(true).build();
        when(authService.sendOtp(anyString(), anyString())).thenReturn(resp);

        String json = "{\"email\":\"test@test.com\",\"purpose\":\"LOGIN\"}";
        mockMvc.perform(post("/auth/send-otp").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendOtp_missingEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\":\"LOGIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyOtp_shouldReturnOtpResponse() throws Exception {
        OtpResponse resp = OtpResponse.builder().success(true).build();
        when(authService.verifyOtp(anyString(), anyString(), anyString())).thenReturn(resp);

        String json = "{\"email\":\"test@test.com\",\"otp\":\"123456\",\"purpose\":\"LOGIN\"}";
        mockMvc.perform(post("/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyOtp_missingOtp_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"purpose\":\"LOGIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resendOtp_shouldReturnOtpResponse() throws Exception {
        OtpResponse resp = OtpResponse.builder().success(true).build();
        when(authService.resendOtp(anyString(), anyString())).thenReturn(resp);

        String json = "{\"email\":\"test@test.com\",\"purpose\":\"LOGIN\"}";
        mockMvc.perform(post("/auth/resend-otp").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getProfile_shouldReturnProfile() throws Exception {
        UserProfileResponse resp = UserProfileResponse.builder().email("user@test.com").build();
        when(authService.getProfile(anyString())).thenReturn(resp);

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(get("/auth/profile").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }

    @Test
    void getProfile_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() throws Exception {
        RegisterRequest req = RegisterRequest.builder().fullName("Updated Name").build();
        UserProfileResponse resp = UserProfileResponse.builder().fullName("Updated Name").build();
        when(authService.updateProfile(anyString(), any())).thenReturn(resp);

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(put("/auth/profile").principal(auth).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    void changePassword_shouldReturnMessage() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass");
        doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(put("/auth/password").principal(auth).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateSubscription_shouldReturnUpdatedProfile() throws Exception {
        SubscriptionUpdateRequest req = new SubscriptionUpdateRequest();
        req.setPlan("PREMIUM");
        UserProfileResponse resp = UserProfileResponse.builder().subscriptionPlan("PREMIUM").build();
        when(authService.updateSubscription(anyString(), anyString())).thenReturn(resp);

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(put("/auth/subscription").principal(auth).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateAccount_shouldReturnMessage() throws Exception {
        doNothing().when(authService).deactivateAccount(anyString());

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(delete("/auth/deactivate").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_shouldReturnList() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of());

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@test.com", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/auth/admin/users").principal(auth))
                .andExpect(status().isOk());
    }

        @Test
        void getAllUsers_withoutAdmin_shouldReturnForbidden() throws Exception {
        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "user@test.com", "password", java.util.Collections.emptyList());

        mockMvc.perform(get("/auth/admin/users").principal(auth))
            .andExpect(status().isForbidden());
        }

    @Test
    void deleteUser_shouldReturnMessage() throws Exception {
        doNothing().when(authService).deleteUserById(anyLong());

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@test.com", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(delete("/auth/admin/users/1").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserSubscription_shouldReturnProfile() throws Exception {
        SubscriptionUpdateRequest req = new SubscriptionUpdateRequest();
        req.setPlan("PREMIUM");
        UserProfileResponse resp = UserProfileResponse.builder().subscriptionPlan("PREMIUM").build();
        when(authService.updateSubscriptionByUserId(anyLong(), anyString())).thenReturn(resp);

        Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@test.com", "password", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(put("/auth/admin/users/1/subscription").principal(auth).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
