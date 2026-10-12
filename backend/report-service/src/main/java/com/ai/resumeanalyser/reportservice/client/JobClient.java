package com.ai.resumeanalyser.reportservice.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


// url=... calls job-service directly by its public URL (no Eureka lookup).
@FeignClient(name = "job-service", url = "${job-service.base-url}")
public interface JobClient {

    @PostMapping("/internal/jobs/search")
    JobSearchResult searchOnce(@RequestBody JobSearchRequest request);

    // Same method name the service already calls. Retries while job-service wakes up
    // (a sleeping free-tier Render service answers 429/502/503/504 or refuses the connection).
    default JobSearchResult search(JobSearchRequest request) {
        final long maxWaitMs = 90_000L;
        final long retryDelayMs = 5_000L;
        final long start = System.currentTimeMillis();
        final long deadline = start + maxWaitMs;
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobClient.class);

        for (int attempt = 1; ; attempt++) {
            try {
                return searchOnce(request);
            } catch (FeignException e) {
                int status = e.status();
                boolean transientError = status == -1 || status == 429 || status == 502
                        || status == 503 || status == 504;

                if (!transientError || System.currentTimeMillis() + retryDelayMs >= deadline) {
                    log.error("job-service search giving up: attempt {}, status {}, after {} ms",
                            attempt, status, System.currentTimeMillis() - start);
                    throw e;
                }

                log.warn("job-service not ready: attempt {}, status {} - retrying in {}s",
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