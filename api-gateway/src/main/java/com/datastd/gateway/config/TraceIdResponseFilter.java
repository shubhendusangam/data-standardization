package com.datastd.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * Injects the current traceId into the HTTP response header {@code X-Trace-Id}.
 * <p>
 * This allows API consumers to grab the trace ID from any response and use it
 * to search logs in Grafana/Loki — for example:
 * <pre>
 *   {service=~".+"} | json | traceId = "abc123..."
 * </pre>
 */
@Configuration
public class TraceIdResponseFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public GlobalFilter traceIdResponseHeaderFilter() {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isBlank()) {
                exchange.getResponse().getHeaders().putIfAbsent(TRACE_ID_HEADER,
                        java.util.List.of(traceId));
            }
        }));
    }
}

