package com.resumeai.jobmatch.config;

/**
 * CORS is handled EXCLUSIVELY by the API-Gateway's CorsFilter.
 * This class intentionally has no CORS configuration to prevent duplicate headers.
 */
public class WebConfig {
    // intentionally empty — CORS handled by api-gateway only
}
