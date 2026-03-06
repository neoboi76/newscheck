package com.newscheck.aggregator.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that protects all /internal/** endpoints with a static API key.
 *
 * Requests must include the header:
 *     X-API-Key: <aggregator.api-key>
 *
 * If the key is not configured (blank), the filter rejects ALL /internal requests
 * with 503 Service Unavailable to prevent accidental open access.
 *
 * Non-/internal requests (e.g., /actuator/health) pass through untouched.
 */
@Component
@Order(1)
@Slf4j
public class ApiKeyAuthFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${aggregator.api-key:}")
    private String configuredApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();

        // Only protect /internal/** paths
        if (!path.startsWith("/internal")) {
            chain.doFilter(request, response);
            return;
        }

        // If no API key is configured, reject with 503 (fail-closed)
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.warn("Rejected request to {} – aggregator.api-key is not configured", path);
            httpResp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"status\":503,\"error\":\"Service Unavailable\","
                  + "\"message\":\"Internal API key not configured\"}");
            return;
        }

        // Validate the provided key
        String providedKey = httpReq.getHeader(API_KEY_HEADER);
        if (providedKey == null || !providedKey.equals(configuredApiKey)) {
            log.warn("Rejected request to {} – invalid or missing API key", path);
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\","
                  + "\"message\":\"Invalid or missing X-API-Key header\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}

