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

// Token-bucket rate limiter for POST /api/auth/** (per client IP)
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
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Token bucket per client IP
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

