package com.ai.resumeanalyser.jobservice.service;

import com.ai.resumeanalyser.jobservice.dto.JobSearchRequest;
import com.ai.resumeanalyser.jobservice.dto.JobSearchResult;

public interface JobSearchService {
    JobSearchResult search(JobSearchRequest request);
}
