package com.resumeai.auth.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client for synchronous calls to AI service APIs. */
@FeignClient(name = "ai-service", fallback = AiServiceClientFallback.class)
public interface AiServiceClient {

    @GetMapping("/api/ai/history/{userId}")
    Map<String, Object> getUserHistory(@PathVariable("userId") Long userId);

    @GetMapping("/api/ai/quota/{userId}")
    Map<String, Object> getUserQuota(@PathVariable("userId") Long userId);

    default int extractHistoryCount(Map<String, Object> payload) {
        if (payload == null) {
            return 0;
        }
        Object data = payload.get("data");
        if (data instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }
}





