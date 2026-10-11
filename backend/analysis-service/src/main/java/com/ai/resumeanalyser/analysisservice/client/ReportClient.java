package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.SaveReportRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// url=... calls report-service directly by its public URL (no Eureka lookup),
// so a sleeping report-service is woken by the request itself.
@FeignClient(name = "report-service", url = "${report-service.base-url}")
public interface ReportClient {

    @PostMapping("/internal/reports")
    void save(@RequestHeader("X-Internal-Api-Key") String internalApiKey, @RequestBody SaveReportRequest request);
}