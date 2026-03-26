package com.datastd.logging;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration that registers {@link RequestLoggingInterceptor}
 * and {@link TraceIdResponseHeaderFilter} for any Spring MVC service
 * that has {@code common-logging} on the classpath.
 */
@Configuration
public class LoggingAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceIdResponseHeaderFilter traceIdResponseHeaderFilter(Tracer tracer) {
        return new TraceIdResponseHeaderFilter(tracer);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .addPathPatterns("/api/**");
    }
}

