package com.ai.resumeanalyser.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String username;
    private Boolean isPrevious;
    private String token;

    public LoginResponse(String username, Boolean isPrevious) {
        this.username = username;
        this.isPrevious = isPrevious;
    }
}
