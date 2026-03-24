package com.datastd.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * Global gateway filter that logs every inbound request and its outcome.
 * The traceId/spanId are automatically placed in the MDC by Micrometer Tracing,
 * so they appear in every log line via the logback pattern.
 */
@Configuration
public class RequestLoggingFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Bean
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            long start = System.currentTimeMillis();

            log.info("→ {} {} from {}",
                    request.getMethod(),
                    request.getURI().getPath(),
                    request.getRemoteAddress());

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - start;
                int status = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value()
                        : 0;
                log.info("← {} {} → {} ({}ms)",
                        request.getMethod(),
                        request.getURI().getPath(),
                        status,
                        duration);
            }));
        };
    }
}

