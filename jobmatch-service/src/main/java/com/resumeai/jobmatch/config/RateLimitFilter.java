package com.resumeai.jobmatch.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final Map<String, Counter> analyzeCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> jobsCounters = new ConcurrentHashMap<>();

    @Value("${app.ratelimit.analyze-per-minute:20}")
    private int analyzeLimit;

    @Value("${app.ratelimit.jobs-per-minute:60}")
    private int jobsLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        if (isAnalyzeEndpoint(path)) {
            if (!allowRequest(analyzeCounters, ip, analyzeLimit)) {
                reject(response, "Rate limit exceeded for analyze endpoint");
                return;
            }
        } else if (isJobsEndpoint(path)) {
            if (!allowRequest(jobsCounters, ip, jobsLimit)) {
                reject(response, "Rate limit exceeded for jobs endpoint");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAnalyzeEndpoint(String path) {
        return path.endsWith("/jobmatch/analyze") || path.endsWith("/jobs/analyze");
    }

    private boolean isJobsEndpoint(String path) {
        return path.endsWith("/jobmatch/jobs") || path.endsWith("/jobmatch/top") || path.endsWith("/jobs/jobs");
    }

    private boolean allowRequest(Map<String, Counter> counters, String key, int limit) {
        long now = Instant.now().toEpochMilli();
        Counter counter = counters.computeIfAbsent(key, k -> new Counter(now));

        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_MILLIS) {
                counter.windowStart = now;
                counter.count.set(0);
            }
            return counter.count.incrementAndGet() <= limit;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private static class Counter {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private Counter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
