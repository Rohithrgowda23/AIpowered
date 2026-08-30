package com.ai.resumeanalyser.reportservice.client;

import java.util.List;

public record JobSearchResult(List<JobDto> jobs, String message) {
}
