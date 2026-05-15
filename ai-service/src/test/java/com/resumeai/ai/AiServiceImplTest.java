package com.resumeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.resumeai.ai.dto.ResumeExtractRequest;
import com.resumeai.ai.service.impl.AiServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AiServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiServiceImpl aiService;

    @Test
    void extractResumeData_whenTextIsEmpty_throwsException() {
        ResumeExtractRequest request = new ResumeExtractRequest();
        request.setResumeText("");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiService.extractResumeData(request);
        });

        assertEquals("PDF TEXT EMPTY", exception.getMessage());
    }

    @Test
    void extractResumeData_whenTextIsNull_throwsException() {
        ResumeExtractRequest request = new ResumeExtractRequest();
        request.setResumeText(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiService.extractResumeData(request);
        });

        assertEquals("PDF TEXT EMPTY", exception.getMessage());
    }
}
