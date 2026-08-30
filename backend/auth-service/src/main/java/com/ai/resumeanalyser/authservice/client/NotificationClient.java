package com.ai.resumeanalyser.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/otp-email")
    void sendOtpEmail(@RequestHeader("X-Internal-Api-Key") String internalApiKey,
                       @RequestBody OtpEmailRequest request);

    record OtpEmailRequest(String username, String email, String otp, String kind) {

    }
}
