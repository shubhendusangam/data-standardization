package com.datastd.quality.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Logs every inbound HTTP request and its response status + latency.
 */
@Configuration
public class RequestLoggingInterceptor implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "reqStartTime";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

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
                long start = (long) request.getAttribute(START_TIME_ATTR);
                long duration = System.currentTimeMillis() - start;
                log.info("← {} {} → {} ({}ms)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), duration);
            }
        }).addPathPatterns("/api/**");
    }
}

