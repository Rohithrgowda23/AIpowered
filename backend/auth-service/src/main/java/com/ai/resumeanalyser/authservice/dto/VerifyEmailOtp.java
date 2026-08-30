package com.ai.resumeanalyser.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailOtp {
    @NotBlank
    private String username;
    @NotBlank @Email
    private String email;
}
