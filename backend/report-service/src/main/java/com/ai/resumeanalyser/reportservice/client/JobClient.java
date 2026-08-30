package com.ai.resumeanalyser.reportservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "job-service")
public interface JobClient {

    @PostMapping("/internal/jobs/search")
    JobSearchResult search(@RequestBody JobSearchRequest request);
}
