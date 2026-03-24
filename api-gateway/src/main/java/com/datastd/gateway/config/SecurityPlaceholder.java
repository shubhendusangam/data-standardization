package com.datastd.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Security Placeholder — v2 Extension Point
 * <p>
 * When authentication is required, add these dependencies to api-gateway/pom.xml:
 * <pre>
 *   spring-boot-starter-security
 *   spring-security-oauth2-resource-server
 *   spring-security-oauth2-jose
 * </pre>
 * <p>
 * Then implement a {@code SecurityWebFilterChain} bean here:
 * <pre>
 * {@literal @}Bean
 * public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
 *     return http
 *         .csrf(ServerHttpSecurity.CsrfSpec::disable)
 *         .authorizeExchange(exchange -> exchange
 *             .pathMatchers("/actuator/**").permitAll()
 *             .anyExchange().authenticated()
 *         )
 *         .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
 *         .build();
 * }
 * </pre>
 * <p>
 * The gateway will validate JWT tokens and propagate the {@code Authorization}
 * header to downstream services automatically (Spring Cloud Gateway forwards
 * all headers by default).
 * <p>
 * Downstream services can then use {@code spring-security-oauth2-resource-server}
 * themselves for fine-grained method-level authorization, or trust the gateway
 * and remain unsecured internally.
 */
@Configuration
public class SecurityPlaceholder {
    // No beans yet — see Javadoc above for the v2 implementation plan.
}

