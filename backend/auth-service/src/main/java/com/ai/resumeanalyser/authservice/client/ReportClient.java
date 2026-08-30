package com.ai.resumeanalyser.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "report-service")
public interface ReportClient {

    @DeleteMapping("/internal/reports/by-email/{email}")
    void deleteByEmail(@RequestHeader("X-Internal-Api-Key") String internalApiKey,
                        @PathVariable("email") String email);
}
