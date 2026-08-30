package com.ai.resumeanalyser.notificationservice.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api-key:}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || "/actuator/health".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader("X-Internal-Api-Key");

        if (expectedKey == null || expectedKey.isBlank() || provided == null || !provided.equals(expectedKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Missing or invalid internal API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
