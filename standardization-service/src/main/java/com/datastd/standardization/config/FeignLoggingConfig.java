package com.datastd.standardization.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Feign clients to log request/response headers and bodies at DEBUG level.
 * Combined with Micrometer tracing ({@code feign.micrometer.enabled=true}), this
 * ensures that every Feign call carries and logs the current traceId/spanId.
 * <p>
 * Feign log levels:
 * <ul>
 *   <li>NONE   — No logging (default)</li>
 *   <li>BASIC  — Request method, URL, response status, execution time</li>
 *   <li>HEADERS— BASIC + request/response headers</li>
 *   <li>FULL   — HEADERS + request/response bodies + metadata</li>
 * </ul>
 */
@Configuration
public class FeignLoggingConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

