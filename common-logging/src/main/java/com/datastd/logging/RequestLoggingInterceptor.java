package com.datastd.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logs every inbound HTTP request and its response status + latency.
 * The traceId/spanId are automatically placed in the MDC by Micrometer Tracing
 * and appear in every log line via the logback pattern.
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "reqStartTime";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        log.info("→ {} {} from {}", request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        Object startTimeAttr = request.getAttribute(START_TIME_ATTR);
        if (startTimeAttr == null) {
            log.info("← {} {} → {} (duration unknown)", request.getMethod(), request.getRequestURI(),
                    response.getStatus());
            return;
        }
        long start = (long) startTimeAttr;
        long duration = System.currentTimeMillis() - start;
        log.info("← {} {} → {} ({}ms)", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);
    }
}

