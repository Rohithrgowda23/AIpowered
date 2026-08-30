package com.ai.resumeanalyser.analysisservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final LightweightJwtValidator validator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("ANALYSIS SERVICE AUTH HEADER: " + authHeader);

        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {

            var email = validator.validateAndGetEmail(authHeader);

            System.out.println("JWT VALIDATION RESULT: " + email);

            email.ifPresent(userEmail -> {

                System.out.println("AUTHENTICATED USER: " + userEmail);

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                userEmail,
                                null,
                                List.of()
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            });
        }

        System.out.println(
                "SECURITY CONTEXT AUTHENTICATION: " +
                        SecurityContextHolder.getContext().getAuthentication()
        );

        filterChain.doFilter(request, response);
    }
}