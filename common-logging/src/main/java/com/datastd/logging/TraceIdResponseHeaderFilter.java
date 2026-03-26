package com.datastd.logging;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds the current traceId to every HTTP response header and to the MDC
 * so that {@code GlobalExceptionHandler} can include it in error response bodies.
 */
public class TraceIdResponseHeaderFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public TraceIdResponseHeaderFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var span = tracer.currentSpan();
        if (span != null && span.context() != null) {
            String traceId = span.context().traceId();
            response.setHeader("X-Trace-Id", traceId);
            MDC.put("traceId", traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}

