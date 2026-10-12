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

    // Retries while report-service answers 429/502/503/504 or refuses the connection,
    // and logs what the failing responses look like so we can see who is answering.
    default void save(String internalApiKey, SaveReportRequest request) {
        final long maxWaitMs = 150_000L;
        final long retryDelayMs = 10_000L;
        final long start = System.currentTimeMillis();
        final long deadline = start + maxWaitMs;
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReportClient.class);

        for (int attempt = 1; ; attempt++) {
            try {
                saveOnce(internalApiKey, request);
                if (attempt > 1) {
                    log.info("report-service save succeeded on attempt {} after {} ms",
                            attempt, System.currentTimeMillis() - start);
                }
                return;
            } catch (FeignException e) {
                int status = e.status();
                boolean transientError = status == -1 || status == 429 || status == 502
                        || status == 503 || status == 504;
                long elapsed = System.currentTimeMillis() - start;

                if (!transientError || System.currentTimeMillis() + retryDelayMs >= deadline) {
                    log.error("report-service save giving up: attempt {}, status {}, after {} ms, response headers {}",
                            attempt, status, elapsed, e.responseHeaders());
                    throw e;
                }

                log.warn("report-service not ready: attempt {}, status {}, after {} ms, response headers {} - retrying in {}s",
                        attempt, status, elapsed, e.responseHeaders(), retryDelayMs / 1000);
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