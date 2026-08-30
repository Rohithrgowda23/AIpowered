package com.ai.resumeanalyser.reportservice.service.implementation;

import com.ai.resumeanalyser.reportservice.client.JobClient;
import com.ai.resumeanalyser.reportservice.client.JobSearchRequest;
import com.ai.resumeanalyser.reportservice.client.JobSearchResult;
import com.ai.resumeanalyser.reportservice.dto.ResultsDto;
import com.ai.resumeanalyser.reportservice.dto.SaveReportRequest;
import com.ai.resumeanalyser.reportservice.entity.PreviousTable;
import com.ai.resumeanalyser.reportservice.repository.PrevTableRepo;
import com.ai.resumeanalyser.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final PrevTableRepo previousTableRepo;
    private final JobClient jobClient;
    private final com.ai.resumeanalyser.reportservice.client.AuthClient authClient;

    @org.springframework.beans.factory.annotation.Value("${internal.api-key:}")
    private String internalApiKey;

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private List<String> nullToEmptyList(List<String> list) {
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public ResponseEntity<?> getLatestReport() {
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();

        PreviousTable previousTable = previousTableRepo.findById(uname).orElse(null);
        if (previousTable == null) {
            return new ResponseEntity<>("No previous Analysis", HttpStatus.NOT_FOUND);
        }

        List<com.ai.resumeanalyser.reportservice.client.JobDto> jobs;
        String jobSearchMessage;

        try {
            JobSearchResult result = jobClient.search(new JobSearchRequest(
                    previousTable.getRoles(),
                    nullToEmptyList(previousTable.getJdSkills()),
                    nullToEmptyList(previousTable.getJdTechnologies()),
                    nullToEmptyList(previousTable.getJdKeywords()),
                    previousTable.getJdLocation()
            ));
            jobs = result.jobs() == null ? new ArrayList<>() : result.jobs();
            jobSearchMessage = result.message();
        } catch (Exception e) {
            log.error("job-service call failed while fetching latest report for {}: {}", uname, e.getMessage());
            jobs = new ArrayList<>();
            jobSearchMessage = "Job recommendations are temporarily unavailable.";
        }

        ResultsDto resultsDto = new ResultsDto(
                previousTable.getScore(),
                previousTable.getAtsoptimizationscore(),
                nullToEmpty(previousTable.getSummary()),
                nullToEmpty(previousTable.getExperienceLevel()),
                nullToEmptyList(previousTable.getSkills()),
                nullToEmptyList(previousTable.getMissingSkills()),
                nullToEmptyList(previousTable.getStrengths()),
                nullToEmptyList(previousTable.getWeaknesses()),
                nullToEmptyList(previousTable.getInterviewTips()),
                nullToEmptyList(previousTable.getPros()),
                nullToEmptyList(previousTable.getCons()),
                nullToEmptyList(previousTable.getSuggestions()),
                jobs,
                nullToEmpty(previousTable.getRoles()),
                nullToEmpty(previousTable.getJobDescription()),
                nullToEmpty(previousTable.getJdExperienceLevel()),
                nullToEmptyList(previousTable.getJdSkills()),
                nullToEmptyList(previousTable.getJdTechnologies()),
                nullToEmptyList(previousTable.getJdKeywords()),
                nullToEmptyList(previousTable.getJdResponsibilities()),
                nullToEmptyList(previousTable.getJdQualifications()),
                nullToEmpty(previousTable.getJdLocation()),
                jobSearchMessage
        );

        return new ResponseEntity<>(resultsDto, HttpStatus.OK);
    }

    @Override
    public void saveReport(SaveReportRequest req) {
        PreviousTable processedData = new PreviousTable(
                req.email(),
                req.score(),
                req.atsoptimizationscore(),
                req.roles(),
                req.summary(),
                req.experienceLevel(),
                req.skills(),
                req.missingSkills(),
                req.strengths(),
                req.weaknesses(),
                req.interviewTips(),
                req.pros(),
                req.cons(),
                req.suggestions(),
                req.jobDescription(),
                req.jdExperienceLevel(),
                req.jdSkills(),
                req.jdTechnologies(),
                req.jdKeywords(),
                req.jdResponsibilities(),
                req.jdQualifications(),
                req.jdLocation()
        );

        previousTableRepo.save(processedData);

        try {
            authClient.markHasPreviousResults(internalApiKey, req.email());
        } catch (Exception e) {

            log.error("Failed to flag previousResults for {} on auth-service: {}", req.email(), e.getMessage());
        }
    }

    @Override
    public void deleteByEmail(String email) {
        previousTableRepo.deleteById(email);
    }
}
