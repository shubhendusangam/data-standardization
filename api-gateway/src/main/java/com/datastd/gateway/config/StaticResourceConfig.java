package com.datastd.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

/**
 * Serves the vanilla HTML/CSS/JS UI from classpath:/static/
 * and redirects the root path "/" to dashboard.html.
 */
@Configuration
public class StaticResourceConfig {

    @Bean
    public RouterFunction<ServerResponse> indexRedirect() {
        return RouterFunctions.route()
                .GET("/", request -> ServerResponse.permanentRedirect(URI.create("/dashboard.html")).build())
                .build();
    }
}

