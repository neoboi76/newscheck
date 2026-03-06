package com.newscheck.newsserver.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates or propagates a correlation ID on every request.
 *
 * How it works:
 *   1. If the incoming request has an X-Correlation-Id header, use it.
 *      (Allows upstream services/gateways to propagate a trace ID.)
 *   2. Otherwise, generate a new UUID.
 *   3. Put the ID into SLF4J MDC so every log line includes it.
 *   4. Echo the ID back in the response header.
 *
 * Logging pattern must include %X{correlationId} to see it in logs.
 * Example: %d{HH:mm:ss} [%X{correlationId}] %-5level %logger{36} - %msg%n
 */
@Component
@Order(0)  // Run before RateLimitFilter and all other filters
public class CorrelationIdFilter implements Filter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY     = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Use existing header or generate new
        String correlationId = httpReq.getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put in MDC for logging
        MDC.put(MDC_KEY, correlationId);

        // Echo back in response
        httpResp.setHeader(HEADER_NAME, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent thread-pool leaks
            MDC.remove(MDC_KEY);
        }
    }
}

