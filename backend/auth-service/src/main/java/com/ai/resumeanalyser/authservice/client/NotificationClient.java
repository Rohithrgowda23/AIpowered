package com.ai.resumeanalyser.authservice.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service",
        url = "${notification-service.base-url}")
public interface NotificationClient {

    @PostMapping("/api/notifications/otp-email")
    void sendOtpEmailOnce(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody OtpEmailRequest request
    );

    // Same method name the service already calls. Retries only while notification-service is
    // still waking up (Render answers 429/502/503/504 or refuses the connection).
    // If notification-service itself answered "Failed to send email" (mail provider problem),
    // or the key is wrong (403), it fails immediately - retrying would not help.
    default void sendOtpEmail(String internalApiKey, OtpEmailRequest request) {
        final long maxWaitMs = 75_000L;
        final long retryDelayMs = 5_000L;
        final long start = System.currentTimeMillis();
        final long deadline = start + maxWaitMs;
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationClient.class);

        for (int attempt = 1; ; attempt++) {
            try {
                sendOtpEmailOnce(internalApiKey, request);
                return;
            } catch (FeignException e) {
                int status = e.status();
                String body = e.contentUTF8();
                boolean answeredByService = body != null && body.contains("Failed to send email");
                boolean transientError = !answeredByService
                        && (status == -1 || status == 429 || status == 502 || status == 503 || status == 504);

                if (!transientError || System.currentTimeMillis() + retryDelayMs >= deadline) {
                    log.error("notification-service OTP email giving up: attempt {}, status {}, after {} ms",
                            attempt, status, System.currentTimeMillis() - start);
                    throw e;
                }

                log.warn("notification-service not ready: attempt {}, status {} - retrying in {}s",
                        attempt, status, retryDelayMs / 1000);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    record OtpEmailRequest(
            String username,
            String email,
            String otp,
            String kind
    ) {
    }
}