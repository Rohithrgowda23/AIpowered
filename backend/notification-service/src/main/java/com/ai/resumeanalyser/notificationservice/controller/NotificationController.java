package com.ai.resumeanalyser.notificationservice.controller;

import com.ai.resumeanalyser.notificationservice.dto.OtpEmailRequest;
import com.ai.resumeanalyser.notificationservice.service.MailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final MailService mailService;

    @PostMapping("/otp-email")
    public ResponseEntity<?> sendOtpEmail(@RequestBody OtpEmailRequest request) {
        try {
            if ("reset".equals(request.kind())) {
                mailService.sentResetOtp(request.username(), request.email(), request.otp());
            } else {
                mailService.sentVerifyOtp(request.username(), request.email(), request.otp());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to send {} OTP email to {}: {}", request.kind(), request.email(), e.getMessage());
            return new ResponseEntity<>("Failed to send email", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
