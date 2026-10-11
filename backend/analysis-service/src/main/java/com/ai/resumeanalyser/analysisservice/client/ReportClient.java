package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.SaveReportRequest;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// url=... calls report-service directly by its public URL (no Eureka lookup).
@FeignClient(name = "report-service", url = "${report-service.base-url}")
public interface ReportClient {

    @PostMapping("/internal/reports")
    void saveOnce(@RequestHeader("X-Internal-Api-Key") String internalApiKey, @RequestBody SaveReportRequest request);

    // Same method name the worker already calls. Retries while report-service wakes up
    // (a sleeping free-tier Render service answers 429/502/503/504 or refuses the connection).
    default void save(String internalApiKey, SaveReportRequest request) {
        final long maxWaitMs = 150_000L;
        final long retryDelayMs = 10_000L;
        final long deadline = System.currentTimeMillis() + maxWaitMs;

        for (int attempt = 1; ; attempt++) {
            try {
                saveOnce(internalApiKey, request);
                return;
            } catch (FeignException e) {
                int status = e.status();
                boolean transientError = status == -1 || status == 429 || status == 502
                        || status == 503 || status == 504;
                if (!transientError || System.currentTimeMillis() + retryDelayMs >= deadline) {
                    throw e;
                }
                org.slf4j.LoggerFactory.getLogger(ReportClient.class).warn(
                        "report-service not ready (attempt {}, status {}) - retrying in {}s",
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
}