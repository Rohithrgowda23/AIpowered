package com.ai.resumeanalyser.authservice.service.implementation;

import com.ai.resumeanalyser.authservice.client.NotificationClient;
import com.ai.resumeanalyser.authservice.client.ReportClient;
import com.ai.resumeanalyser.authservice.dto.*;
import com.ai.resumeanalyser.authservice.entity.OtpVerify;
import com.ai.resumeanalyser.authservice.entity.UsersTable;
import com.ai.resumeanalyser.authservice.repository.OtpVerifyRepo;
import com.ai.resumeanalyser.authservice.repository.UsersTableRepo;
import com.ai.resumeanalyser.authservice.service.JwtService;
import com.ai.resumeanalyser.authservice.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationProvider authenticationProvider;
    private final UsersTableRepo usersTableRepository;
    private final OtpVerifyRepo otpVerifyRepository;
    private final NotificationClient notificationClient;
    private final ReportClient reportClient;

    @Value("${internal.api-key:}")
    private String internalApiKey;

    @Override
    public ResponseEntity<?> register(UserRegister reg) {

        OtpVerify verify = otpVerifyRepository.findById(reg.getEmail()).orElse(null);

        if (verify == null) {
            return new ResponseEntity<>("Unauthorised request", HttpStatus.UNAUTHORIZED);
        }
        if (!verify.getVerifyOtp().equals(reg.getVerifyotp())) {
            return new ResponseEntity<>("Invalid OTP", HttpStatus.NOT_ACCEPTABLE);
        }
        if (verify.getVerifyExpiration().before(new Date())) {
            log.warn("OTP expired for email {}", reg.getEmail());
            return new ResponseEntity<>("OTP expired", HttpStatus.NOT_ACCEPTABLE);
        }

        if (!usersTableRepository.existsById(reg.getEmail())) {
            UsersTable newUser = UsersTable.builder()
                    .username(reg.getUsername())
                    .email(reg.getEmail())
                    .password(passwordEncoder.encode(reg.getPassword()))
                    .previousResults(false)
                    .resetOtp(null)
                    .resetExpiration(null)
                    .build();
            usersTableRepository.save(newUser);
            otpVerifyRepository.deleteById(reg.getEmail());
            return new ResponseEntity<>("Successfully created for " + newUser.getUsername(), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("User already exist", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public ResponseEntity<?> verifyEmail(VerifyEmailOtp verifyEmail) {

        if (usersTableRepository.existsById(verifyEmail.getEmail())) {
            return new ResponseEntity<>("Email already Registered", HttpStatus.CONFLICT);
        }

        SecureRandom secure = new SecureRandom();
        String otp = String.valueOf(secure.nextInt(900000) + 100000);
        OtpVerify otpverify = new OtpVerify(
                verifyEmail.getEmail(), otp, new Date(System.currentTimeMillis() + 10 * 60 * 1000));

        // Save first so the OTP exists even if the downstream email call is slow/fails.
        otpVerifyRepository.save(otpverify);

        try {
            notificationClient.sendOtpEmail(internalApiKey,
                    new NotificationClient.OtpEmailRequest(verifyEmail.getUsername(), verifyEmail.getEmail(), otp, "verify"));
            return new ResponseEntity<>("OTP sent successfully", HttpStatus.OK);
        } catch (Exception e) {
            otpVerifyRepository.deleteById(verifyEmail.getEmail());
            log.error("notification-service call failed for verify-email OTP ({}): {}", verifyEmail.getEmail(), e.getMessage());
            return new ResponseEntity<>("Failed to send OTP. Check your email address.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ResponseEntity<?> login(UserLogin req) {
        try {
            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
            String token = jwtService.generateToken(req.getEmail());
            UsersTable user = usersTableRepository.findById(req.getEmail()).orElse(null);

            LoginResponse loginRes = new LoginResponse();
            loginRes.setUsername(user.getUsername());
            loginRes.setIsPrevious(user.getPreviousResults());
            loginRes.setToken(token);

            return new ResponseEntity<>(loginRes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public ResponseEntity<?> sentResetOtp(ResetOtp req) {
        UsersTable user = usersTableRepository.findById(req.getEmail()).orElse(null);
        if (user == null) {
            return new ResponseEntity<>("Invalid Email address", HttpStatus.UNAUTHORIZED);
        }
        try {
            SecureRandom secure = new SecureRandom();
            String otp = String.valueOf(secure.nextInt(900000) + 100000);
            user.setResetOtp(otp);
            user.setResetExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
            usersTableRepository.save(user);
            notificationClient.sendOtpEmail(internalApiKey,
                    new NotificationClient.OtpEmailRequest(user.getUsername(), req.getEmail(), otp, "reset"));
            return new ResponseEntity<>("OTP sent successfully", HttpStatus.OK);
        } catch (Exception e) {
            log.error("notification-service call failed for reset OTP ({}): {}", req.getEmail(), e.getMessage());
            return new ResponseEntity<>("Couldn't sent OTP", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ResponseEntity<?> verifyResetOtp(ResetOtpVerification req) {
        UsersTable user = usersTableRepository.findById(req.getEmail()).orElse(null);
        if (user == null) {
            return new ResponseEntity<>("Unauthorised request", HttpStatus.UNAUTHORIZED);
        }
        if (!user.getResetOtp().equals(req.getOtp())) {
            return new ResponseEntity<>("Invalid OTP", HttpStatus.NOT_ACCEPTABLE);
        }
        if (user.getResetExpiration().before(new Date(System.currentTimeMillis()))) {
            return new ResponseEntity<>("OTP Expired", HttpStatus.NOT_ACCEPTABLE);
        }
        return new ResponseEntity<>("Verified OTP", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> resetAccountPassword(ResetPasscode req) {
        UsersTable user = usersTableRepository.findById(req.getEmail()).orElse(null);
        if (user == null) {
            return new ResponseEntity<>("Unauthorised request", HttpStatus.UNAUTHORIZED);
        }
        if (!user.getResetOtp().equals(req.getOtp())) {
            return new ResponseEntity<>("Invalid OTP", HttpStatus.NOT_ACCEPTABLE);
        }
        if (user.getResetExpiration().before(new Date(System.currentTimeMillis()))) {
            return new ResponseEntity<>("OTP Expired", HttpStatus.NOT_ACCEPTABLE);
        }
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setResetOtp(null);
        user.setResetExpiration(null);
        usersTableRepository.save(user);
        return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> logout() {
        HttpHeaders headers = new HttpHeaders();
        ResponseCookie cookie = ResponseCookie.from("entrypasstoken", "")
                .httpOnly(true).secure(false).sameSite("Strict").maxAge(0).path("/").build();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return new ResponseEntity<>("Successfully loggedOut", headers, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> deleteAccount() {
        try {
            String uname = SecurityContextHolder.getContext().getAuthentication().getName();
            usersTableRepository.deleteById(uname);

            try {
                reportClient.deleteByEmail(internalApiKey, uname);
            } catch (Exception e) {
                // Don't fail account deletion just because report-service was briefly
                // unreachable - the user's auth record IS gone; log loudly so orphaned
                // report data can be cleaned up rather than silently losing the signal.
                log.error("Failed to delete report data for {} during account deletion - orphaned report may remain: {}",
                        uname, e.getMessage());
            }

            HttpHeaders headers = new HttpHeaders();
            ResponseCookie cookie = ResponseCookie.from("entrypasstoken", "")
                    .httpOnly(true).secure(false).sameSite("Strict").maxAge(0).path("/").build();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            return new ResponseEntity<>("Account deleted successfully", headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to delete", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<?> validateToken() {
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            UsersTable user = usersTableRepository.findById(name)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            LoginResponse loginRes = new LoginResponse(user.getUsername(), user.getPreviousResults());
            return new ResponseEntity<>(loginRes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }
}
