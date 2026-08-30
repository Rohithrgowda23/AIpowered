package com.ai.resumeanalyser.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasscode {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String otp;
    @NotBlank
    private String password;
}
