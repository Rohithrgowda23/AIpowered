package com.ai.resumeanalyser.authservice.controller;

import com.ai.resumeanalyser.authservice.dto.*;
import com.ai.resumeanalyser.authservice.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailOtp request) {
        return securityService.verifyEmail(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegister request) {
        return securityService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLogin request) {
        return securityService.login(request);
    }

    @PostMapping("/send-reset-otp")
    public ResponseEntity<?> sendResetOtp(@Valid @RequestBody ResetOtp request) {
        return securityService.sentResetOtp(request);
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@Valid @RequestBody ResetOtpVerification request) {
        return securityService.verifyResetOtp(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasscode request) {
        return securityService.resetAccountPassword(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return securityService.logout();
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount() {
        return securityService.deleteAccount();
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken() {
        return securityService.validateToken();
    }
}
