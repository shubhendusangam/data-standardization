package com.datastd.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RequestLoggingInterceptor}.
 */
class RequestLoggingInterceptorTest {

    private RequestLoggingInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RequestLoggingInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void preHandle_setsStartTimeAttributeAndReturnsTrue() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/datasets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result, "preHandle should always return true");
        verify(request).setAttribute(eq("reqStartTime"), anyLong());
    }

    @Test
    void afterCompletion_logsResponseWithDuration() {
        long startTime = System.currentTimeMillis() - 50;
        when(request.getAttribute("reqStartTime")).thenReturn(startTime);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/ingestion/upload");
        when(response.getStatus()).thenReturn(200);

        // Should not throw
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, new Object(), null));
    }

    @Test
    void afterCompletion_handlesExceptionParameter() {
        long startTime = System.currentTimeMillis() - 10;
        when(request.getAttribute("reqStartTime")).thenReturn(startTime);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/rules");
        when(response.getStatus()).thenReturn(500);

        // Should not throw even when an exception is passed
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, new Object(),
                        new RuntimeException("test error")));
    }
}

