package com.ai.resumeanalyser.authservice.service;

import io.jsonwebtoken.Claims;

import java.util.Date;
import java.util.function.Function;

public interface JwtService {
    String generateToken(String email);
    Claims extractAllClaims(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimResolver);
    String getEmail(String token);
    Date getExpiration(String token);
    Boolean validateToken(String token, String email);
}
