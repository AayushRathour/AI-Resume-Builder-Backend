package com.resumeai.template.config;

/**
 * CORS is handled EXCLUSIVELY by the API-Gateway's CorsFilter.
 *
 * Do NOT add any CORS configuration here. Adding it here causes
 * the 'Access-Control-Allow-Origin' header to appear twice
 * (once from the gateway, once from this service), which browsers
 * reject with ERR_FAILED / CORS policy errors.
 *
 * This class intentionally left with no CORS beans.
 */
public class CorsConfig {
    // intentionally empty — CORS handled by api-gateway only
}
