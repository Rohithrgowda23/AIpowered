package com.ai.resumeanalyser.jobservice.service.implementation;

import com.ai.resumeanalyser.jobservice.dto.JobDto;
import com.ai.resumeanalyser.jobservice.dto.JobSearchRequest;
import com.ai.resumeanalyser.jobservice.dto.JobSearchResult;
import com.ai.resumeanalyser.jobservice.external.Job;
import com.ai.resumeanalyser.jobservice.external.JobSearchResponse;
import com.ai.resumeanalyser.jobservice.service.JobSearchService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobSearchServiceImpl implements JobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JobSearchServiceImpl.class);
    private static final int MAX_QUERY_TERMS = 4;

    @Value("${adzuna.app-id}")
    private String adzunaAppId;

    @Value("${adzuna.app-key}")
    private String adzunaAppKey;

    @Value("${adzuna.country}")
    private String adzunaCountry;

    @Override
    public JobSearchResult search(JobSearchRequest request) {
        AdzunaSearchOutcome outcome = fetchAdzunaJobsWithFallback(
                request.role(), request.jdSkills(), request.jdTechnologies(), request.jdKeywords(), request.jdLocation());

        List<JobDto> dtos = outcome.getJobs().stream().map(this::toDto).toList();
        return new JobSearchResult(dtos, outcome.getMessage());
    }

    private JobDto toDto(Job job) {
        return new JobDto(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getRedirect_url(),
                job.getCompany() != null ? new JobDto.CompanyDto(job.getCompany().getDisplay_name()) : null,
                job.getLocation() != null ? new JobDto.LocationDto(job.getLocation().getDisplay_name()) : null,
                job.getCategory() != null ? new JobDto.CategoryDto(job.getCategory().getTag(), job.getCategory().getLabel()) : null
        );
    }

    private AdzunaSearchOutcome fetchAdzunaJobsWithFallback(String jobRole,
                                                              List<String> jdSkills,
                                                              List<String> jdTechnologies,
                                                              List<String> jdKeywords,
                                                              String jdLocation) {

        String primaryTerm = jobRole == null ? "" : jobRole.trim();
        if (primaryTerm.isEmpty()) {
            return new AdzunaSearchOutcome(new ArrayList<>(), "No job role available to search for.");
        }

        List<String> broadeningTerms = mergeBroadeningTerms(jdSkills, jdTechnologies, jdKeywords);

        // Attempt 1: Job Role (what) + JD-derived terms (what_or) + JD location (where)
        AdzunaAttemptResult attempt1 = callAdzuna(primaryTerm, broadeningTerms, jdLocation, 1);
        if (attempt1.isSuccess() && !attempt1.getJobs().isEmpty()) {
            return new AdzunaSearchOutcome(attempt1.getJobs(), null);
        }

        // Attempt 2: drop location - it may be too narrow / unrecognised in this Adzuna country index.
        if (jdLocation != null && !jdLocation.isBlank()) {
            AdzunaAttemptResult attempt2 = callAdzuna(primaryTerm, broadeningTerms, null, 2);
            if (attempt2.isSuccess() && !attempt2.getJobs().isEmpty()) {
                return new AdzunaSearchOutcome(attempt2.getJobs(), null);
            }
        }

        // Attempt 3: Job Role only - identical to the original, pre-Job-Description behaviour.
        if (!broadeningTerms.isEmpty()) {
            AdzunaAttemptResult attempt3 = callAdzuna(primaryTerm, new ArrayList<>(), null, 3);
            if (attempt3.isSuccess() && !attempt3.getJobs().isEmpty()) {
                return new AdzunaSearchOutcome(attempt3.getJobs(), null);
            }
            if (!attempt3.isSuccess()) {
                return new AdzunaSearchOutcome(new ArrayList<>(),
                        "Job search is temporarily unavailable. Please try again later.");
            }
        } else if (!attempt1.isSuccess()) {
            return new AdzunaSearchOutcome(new ArrayList<>(),
                    "Job search is temporarily unavailable. Please try again later.");
        }

        return new AdzunaSearchOutcome(new ArrayList<>(),
                "No matching jobs were found right now for \"" + primaryTerm +
                        "\". Try broadening the job description or role and re-analyse your resume.");
    }

    private AdzunaAttemptResult callAdzuna(String primaryTerm, List<String> orTerms, String location, int attemptNumber) {
        RestTemplate restTemplate = new RestTemplate();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.adzuna.com/v1/api/jobs/" + adzunaCountry + "/search/1")
                .queryParam("app_id", adzunaAppId)
                .queryParam("app_key", adzunaAppKey)
                .queryParam("what", primaryTerm)
                .queryParam("results_per_page", 10)
                .queryParam("content-type", "application/json");

        if (!orTerms.isEmpty()) {
            builder.queryParam("what_or", String.join(" ", orTerms));
        }
        if (location != null && !location.isBlank()) {
            builder.queryParam("where", location);
        }

        URI uri = builder.build().encode().toUri();

        String maskedUrl = uri.toString().replaceAll("app_key=[^&]+", "app_key=***");
        log.info("Adzuna request (attempt {}): {}", attemptNumber, maskedUrl);

        try {
            ResponseEntity<JobSearchResponse> response = restTemplate.getForEntity(uri, JobSearchResponse.class);

            JobSearchResponse body = response.getBody();
            List<Job> jobs = (body != null && body.getResults() != null) ? body.getResults() : new ArrayList<>();

            log.info("Adzuna response (attempt {}): status={}, totalCount={}, resultsReturned={}",
                    attemptNumber, response.getStatusCode(), body != null ? body.getCount() : 0, jobs.size());

            return new AdzunaAttemptResult(true, jobs);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Adzuna HTTP error (attempt {}): status={}, body={}",
                    attemptNumber, e.getStatusCode(), e.getResponseBodyAsString());
            return new AdzunaAttemptResult(false, new ArrayList<>());
        } catch (Exception e) {
            log.error("Adzuna call failed (attempt {}): {}", attemptNumber, e.getMessage(), e);
            return new AdzunaAttemptResult(false, new ArrayList<>());
        }
    }

    private List<String> mergeBroadeningTerms(List<String> a, List<String> b, List<String> c) {
        List<String> merged = new ArrayList<>();
        for (List<String> src : List.of(nullToEmptyList(a), nullToEmptyList(b), nullToEmptyList(c))) {
            for (String term : src) {
                if (term == null) continue;
                String t = term.trim();
                if (t.isEmpty()) continue;
                boolean alreadyPresent = merged.stream().anyMatch(m -> m.equalsIgnoreCase(t));
                if (!alreadyPresent) {
                    merged.add(t);
                }
                if (merged.size() >= MAX_QUERY_TERMS) {
                    return merged;
                }
            }
        }
        return merged;
    }

    private List<String> nullToEmptyList(List<String> value) {
        return value == null ? new ArrayList<>() : value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class AdzunaAttemptResult {
        private boolean success;
        private List<Job> jobs;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class AdzunaSearchOutcome {
        private List<Job> jobs;
        private String message;
    }
}
