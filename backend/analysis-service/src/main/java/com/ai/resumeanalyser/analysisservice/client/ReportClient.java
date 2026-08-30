package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.SaveReportRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "report-service")
public interface ReportClient {

    @PostMapping("/internal/reports")
    void save(@RequestHeader("X-Internal-Api-Key") String internalApiKey, @RequestBody SaveReportRequest request);
}
