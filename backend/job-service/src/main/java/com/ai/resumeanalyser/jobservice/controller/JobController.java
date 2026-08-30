package com.ai.resumeanalyser.jobservice.controller;

import com.ai.resumeanalyser.jobservice.dto.JobSearchRequest;
import com.ai.resumeanalyser.jobservice.dto.JobSearchResult;
import com.ai.resumeanalyser.jobservice.service.JobSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobSearchService jobSearchService;

    @PostMapping("/search")
    public JobSearchResult search(@RequestBody JobSearchRequest request) {
        return jobSearchService.search(request);
    }
}
