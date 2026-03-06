package com.newscheck.newsserver.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiter for authentication endpoints.
 *
 * Uses a simple token-bucket algorithm per client IP:
 *   - Each IP gets {@code max-requests} tokens per {@code window-seconds} window.
 *   - When tokens are exhausted, requests are rejected with 429 Too Many Requests.
 *   - Tokens refill completely when the window expires.
 *
 * Only applies to POST /api/auth/** (login + register).
 * All other endpoints pass through unaffected.
 *
 * This prevents brute-force password guessing without requiring
 * an external cache like Redis.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter implements Filter {

    @Value("${rate-limit.auth.max-requests:10}")
    private int maxRequests;

    @Value("${rate-limit.auth.window-seconds:60}")
    private int windowSeconds;

    private final Map<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Only rate-limit auth endpoints
        String path   = httpReq.getRequestURI();
        String method = httpReq.getMethod();
        if (!(path.startsWith("/api/auth") && "POST".equalsIgnoreCase(method))) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpReq);
        ClientBucket bucket = buckets.computeIfAbsent(clientIp, k -> new ClientBucket());

        if (bucket.tryConsume(maxRequests, windowSeconds)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on {}", clientIp, path);
            httpResp.setStatus(429);
            httpResp.setContentType("application/json");
            httpResp.setHeader("Retry-After", String.valueOf(windowSeconds));
            httpResp.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                  + "\"message\":\"Rate limit exceeded. Try again in " + windowSeconds + " seconds.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // Support reverse-proxy X-Forwarded-For header
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple token-bucket: tracks remaining tokens and window expiry per client.
     * Thread-safe via synchronized methods (low contention — only auth endpoints).
     */
    private static class ClientBucket {
        private final AtomicLong windowStart = new AtomicLong(0);
        private int remaining = 0;

        synchronized boolean tryConsume(int maxRequests, int windowSeconds) {
            long now = System.currentTimeMillis();
            long windowMs = windowSeconds * 1000L;

            // Window expired → reset
            if (now - windowStart.get() > windowMs) {
                windowStart.set(now);
                remaining = maxRequests;
            }

            if (remaining > 0) {
                remaining--;
                return true;
            }
            return false;
        }
    }
}

