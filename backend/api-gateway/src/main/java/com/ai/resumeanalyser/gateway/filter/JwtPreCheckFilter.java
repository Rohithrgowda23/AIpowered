package com.ai.resumeanalyser.gateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Component
public class JwtPreCheckFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtPreCheckFilter.class);

    @Value("${JWT_SECRET:}")
    private String jwtSecret;

    // Paths that must work WITHOUT a token.
    // /actuator/health* is needed so Render's health check (and uptime pingers) get a 200.
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/",
            "/oauth2/",
            "/login/oauth2/",
            "/actuator/health",
            "/actuator/info"
    );

    @Override
    public int getOrder() {
        return -1; // run before routing
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        boolean isPublic = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Never log the header value itself - it contains the user's JWT.
            log.debug("Gateway: no bearer token on {}", path);
            return unauthorized(exchange, "Missing bearer token");
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            log.error("JWT_SECRET is not configured on api-gateway - refusing to validate tokens");
            return unauthorized(exchange, "Authentication temporarily unavailable");
        }

        String token = authHeader.substring(7);
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token); // throws if signature invalid or token expired
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Gateway JWT rejected for {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBufferFactory bufferFactory = response.bufferFactory();
        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        DataBuffer buffer = bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}