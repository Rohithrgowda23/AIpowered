package com.ai.resumeanalyser.notificationservice.dto;

public record OtpEmailRequest(String username, String email, String otp, String kind) {
}
