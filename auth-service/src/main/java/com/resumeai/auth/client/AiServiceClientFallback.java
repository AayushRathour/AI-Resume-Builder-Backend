package com.resumeai.auth.client;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Fallback behavior for AI service call failures. */
@Component
@Slf4j
public class AiServiceClientFallback implements AiServiceClient {

    @Override
    public Map<String, Object> getUserHistory(Long userId) {
        log.warn("AiServiceClient fallback for user history userId={}", userId);
        Map<String, Object> out = new HashMap<>();
        out.put("status", "failed");
        out.put("message", "AI temporarily unavailable");
        out.put("data", java.util.List.of());
        return out;
    }

    @Override
    public Map<String, Object> getUserQuota(Long userId) {
        log.warn("AiServiceClient fallback for user quota userId={}", userId);
        Map<String, Object> out = new HashMap<>();
        out.put("status", "failed");
        out.put("message", "AI temporarily unavailable");
        out.put("data", Map.of());
        return out;
    }
}





