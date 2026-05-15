package com.resumeai.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileSizeLimitFilterTest {

    @InjectMocks
    private FileSizeLimitFilter fileSizeLimitFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
    }

    @Test
    void doFilterInternal_NotMultipart_Success() throws ServletException, IOException {
        when(request.getContentType()).thenReturn("application/json");

        fileSizeLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MultipartSmallFile_Success() throws ServletException, IOException {
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=something");
        when(request.getContentLengthLong()).thenReturn(1024L); // 1KB

        fileSizeLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MultipartLargeFile_Returns413() throws ServletException, IOException {
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=something");
        when(request.getContentLengthLong()).thenReturn(6L * 1024 * 1024); // 6MB

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        fileSizeLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(413);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }
}
