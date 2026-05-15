package com.resumeai.auth.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import com.resumeai.auth.entity.Provider;
import com.resumeai.auth.entity.Role;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "oauthSuccessRedirectUrl", "http://success");
        ReflectionTestUtils.setField(handler, "oauthFailureRedirectUrl", "http://failure");
    }

    @Test
    void shouldRedirectFailureIfNoEmail() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(new HashMap<>());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("error=Email"));
    }

    @Test
    void shouldCreateNewUserAndRedirectSuccess() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "test@test.com");
        attrs.put("name", "Test Name");
        
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attrs);
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        
        User savedUser = User.builder()
                .email("test@test.com")
                .fullName("Test Name")
                .provider(Provider.GOOGLE)
                .role(Role.USER)
                .isActive(true)
                .build();
                
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(response).sendRedirect("http://success?token=fake-jwt-token");
    }

    @Test
    void shouldRedirectSuccessForExistingUser() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "existing@test.com");
        attrs.put("name", "Existing Name");

        User existing = User.builder()
                .email("existing@test.com")
                .fullName("Existing Name")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attrs);
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-existing");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://success?token=jwt-existing");
    }

    @Test
    void shouldRedirectFailureForSuspendedUser() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "suspended@test.com");
        attrs.put("name", "Suspended Name");

        User suspended = User.builder()
                .email("suspended@test.com")
                .fullName("Suspended Name")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .isActive(false)
                .isDeleted(false)
                .build();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attrs);
        when(userRepository.findByEmail("suspended@test.com")).thenReturn(Optional.of(suspended));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("Account+is+suspended"));
    }

    @Test
    void shouldRedirectFailureForDeletedUser() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "deleted@test.com");
        attrs.put("name", "Deleted Name");

        User deleted = User.builder()
                .email("deleted@test.com")
                .fullName("Deleted Name")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .isDeleted(true)
                .build();

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(attrs);
        when(userRepository.findByEmail("deleted@test.com")).thenReturn(Optional.of(deleted));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(contains("Account+is+deleted"));
    }
}
