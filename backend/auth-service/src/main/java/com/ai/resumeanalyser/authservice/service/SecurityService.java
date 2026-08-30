package com.ai.resumeanalyser.authservice.service;

import com.ai.resumeanalyser.authservice.dto.*;
import org.springframework.http.ResponseEntity;

public interface SecurityService {
    ResponseEntity<?> register(UserRegister reg);
    ResponseEntity<?> verifyEmail(VerifyEmailOtp verifyEmail);
    ResponseEntity<?> login(UserLogin req);
    ResponseEntity<?> sentResetOtp(ResetOtp req);
    ResponseEntity<?> verifyResetOtp(ResetOtpVerification req);
    ResponseEntity<?> resetAccountPassword(ResetPasscode req);
    ResponseEntity<?> logout();
    ResponseEntity<?> deleteAccount();
    ResponseEntity<?> validateToken();
}
