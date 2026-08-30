package com.ai.resumeanalyser.jobservice.dto;

import java.util.List;

public record JobSearchResult(List<JobDto> jobs, String message) {
}
