package com.ai.resumeanalyser.notificationservice.service;

import jakarta.mail.MessagingException;

public interface MailService {
    void sentVerifyOtp(String username, String email, String otp) throws MessagingException;
    void sentResetOtp(String username, String email, String otp) throws MessagingException;
}
