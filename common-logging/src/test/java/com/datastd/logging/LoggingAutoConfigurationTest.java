package com.datastd.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LoggingAutoConfiguration}.
 */
class LoggingAutoConfigurationTest {

    @Test
    void requestLoggingInterceptor_returnsNewInstance() {
        LoggingAutoConfiguration config = new LoggingAutoConfiguration();
        RequestLoggingInterceptor interceptor = config.requestLoggingInterceptor();
        assertNotNull(interceptor, "Should create a non-null RequestLoggingInterceptor");
    }

    @Test
    void requestLoggingInterceptor_returnsDistinctInstances() {
        LoggingAutoConfiguration config = new LoggingAutoConfiguration();
        RequestLoggingInterceptor a = config.requestLoggingInterceptor();
        RequestLoggingInterceptor b = config.requestLoggingInterceptor();
        assertNotSame(a, b, "Each call should return a new instance (Spring manages singleton scope)");
    }
}

