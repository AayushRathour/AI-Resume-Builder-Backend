package com.resumeai.jobmatch.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "analyzeLimit", 20);
        ReflectionTestUtils.setField(filter, "jobsLimit", 60);
    }

    @Test
    void testDoFilterInternal_success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setRequestURI("/api/v1/jobmatch/analyze");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testDoFilterInternal_rateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.2");
        request.setRequestURI("/api/v1/jobmatch/analyze");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Max requests is 20
        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(request, response, chain);
        }

        // 21st request should be blocked
        filter.doFilterInternal(request, response, chain);
        
        assertEquals(429, response.getStatus());
        verify(chain, times(20)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_jobsEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/jobmatch/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testResolveClientIp_forwarded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        
        String ip = (String) ReflectionTestUtils.invokeMethod(filter, "resolveClientIp", request);
        assertEquals("10.0.0.1", ip);
    }

    @Test
    void testAllowRequest_windowReset() throws Exception {
        ReflectionTestUtils.setField(filter, "analyzeLimit", 1);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/jobmatch/analyze");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain); // OK
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        filter.doFilterInternal(request, resp2, chain); // Blocked
        assertEquals(429, resp2.getStatus());
    }
}
