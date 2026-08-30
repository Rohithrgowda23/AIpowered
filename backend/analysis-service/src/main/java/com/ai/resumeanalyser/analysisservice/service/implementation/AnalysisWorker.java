package com.ai.resumeanalyser.analysisservice.service.implementation;

import com.ai.resumeanalyser.analysisservice.client.ReportClient;
import com.ai.resumeanalyser.analysisservice.client.ResumeServiceClient;
import com.ai.resumeanalyser.analysisservice.dto.JdInsights;
import com.ai.resumeanalyser.analysisservice.dto.ResumeParseResponse;
import com.ai.resumeanalyser.analysisservice.dto.SaveReportRequest;
import com.ai.resumeanalyser.analysisservice.entity.AnalysisStatus;
import com.ai.resumeanalyser.analysisservice.prompts.AnalysisPrompts;
import com.ai.resumeanalyser.analysisservice.repository.AnalysisJobRepo;
import com.ai.resumeanalyser.analysisservice.util.JsonHelpers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisWorker {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWorker.class);

    @Value("${genKey}")
    private String genKey;

    @Value("${internal.api-key:}")
    private String internalApiKey;

    private static final int MAX_GEMINI_ATTEMPTS = 3;

    private final AnalysisJobRepo analysisJobRepo;
    private final ResumeServiceClient resumeServiceClient;
    private final ReportClient reportClient;

    @Async("resumeAnalysisExecutor")
    public void processAnalysisAsync(String jobId, String uname, String bearerToken, String normalizedRoles,
                                      String normalizedJobDescription, String fileName, byte[] fileBytes) {

        try {
            updateJob(jobId, AnalysisStatus.PARSING_RESUME, 15, "Reading your resume...", null);

            ResumeParseResponse parsed;
            try {
                parsed = resumeServiceClient.uploadAndParse(bearerToken, fileName, fileBytes);
            } catch (Exception e) {
                log.error("resume-service call failed (job {}): {}", jobId, e.getMessage());
                updateJob(jobId, AnalysisStatus.FAILED, 0, null,
                        "We couldn't read your resume file right now. Please try again.");
                return;
            }
            String extracted = parsed.extractedText();

            String jobDescriptionForPrompt = normalizedJobDescription.isEmpty() ? "None provided" : normalizedJobDescription;
            String promptText = String.format(
                    AnalysisPrompts.RESUME_ANALYSIS_PROMPT_TEMPLATE, normalizedRoles, extracted, jobDescriptionForPrompt);

            updateJob(jobId, AnalysisStatus.ANALYZING_WITH_AI, 40, "AI is analysing your resume...", null);

            String results = null;
            Client client = Client.builder().apiKey(genKey).build();
            Content content = Content.builder().parts(Part.fromText(promptText)).build();

            Exception lastException = null;
            for (int attempt = 1; attempt <= MAX_GEMINI_ATTEMPTS; attempt++) {
                try {
                    GenerateContentResponse response = client.models.generateContent(
                            "gemini-2.5-flash", content, GenerateContentConfig.builder().temperature(0.0f).build());
                    results = response.text();
                    lastException = null;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    boolean rateLimited = e.getMessage() != null && e.getMessage().contains("429");
                    log.error("Gemini call failed (job {}), attempt {}{}", jobId, attempt,
                            rateLimited ? " [rate limited]" : "", e);
                    if (attempt < MAX_GEMINI_ATTEMPTS) {
                        Thread.sleep(rateLimited ? 10_000L : 1500L);
                    }
                }
            }

            if (lastException != null || results == null) {
                boolean rateLimited = lastException != null && lastException.getMessage() != null
                        && lastException.getMessage().contains("429");
                String message = rateLimited
                        ? "Resume analysis service is rate-limited right now. Please wait a minute and try again."
                        : "Resume analysis service is temporarily unavailable. Please try again.";
                updateJob(jobId, AnalysisStatus.FAILED, 0, null, message);
                return;
            }

            updateJob(jobId, AnalysisStatus.PROCESSING_RESULT, 70, "Processing AI results...", null);

            int firstBrace = results.indexOf("{");
            int lastBrace = results.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                results = results.substring(firstBrace, lastBrace + 1);
            }

            JsonNode node;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                node = objectMapper.readTree(results);
            } catch (Exception e) {
                log.error("Failed to parse Gemini response as JSON (job {}): {}", jobId, e.getMessage());
                updateJob(jobId, AnalysisStatus.FAILED, 0, null,
                        "We couldn't process the AI's response. Please try again.");
                return;
            }

            int score = JsonHelpers.safeInt(node, "score");
            int atsScore = JsonHelpers.safeInt(node, "atsoptimizationscore");

            if (score == 0) {
                updateJob(jobId, AnalysisStatus.FAILED, 0, null,
                        "This resume doesn't appear relevant to the role entered, or couldn't be read as a resume. Please check both and try again.");
                return;
            }

            updateJob(jobId, AnalysisStatus.SAVING_REPORT, 90, "Saving your report...", null);

            JdInsights jdInsights = new JdInsights(
                    JsonHelpers.safeText(node, "jdExperienceLevel"),
                    JsonHelpers.safeStringList(node, "jdSkills"),
                    JsonHelpers.safeStringList(node, "jdTechnologies"),
                    JsonHelpers.safeStringList(node, "jdKeywords"),
                    JsonHelpers.safeStringList(node, "jdResponsibilities"),
                    JsonHelpers.safeStringList(node, "jdQualifications"),
                    JsonHelpers.safeText(node, "jdLocation")
            );

            SaveReportRequest saveRequest = new SaveReportRequest(
                    uname,
                    score,
                    atsScore,
                    normalizedRoles,
                    JsonHelpers.safeText(node, "summary"),
                    JsonHelpers.safeText(node, "experienceLevel"),
                    JsonHelpers.safeStringList(node, "skills"),
                    JsonHelpers.safeStringList(node, "missingSkills"),
                    JsonHelpers.safeStringList(node, "strengths"),
                    JsonHelpers.safeStringList(node, "weaknesses"),
                    JsonHelpers.safeStringList(node, "interviewTips"),
                    JsonHelpers.safeStringList(node, "pros"),
                    JsonHelpers.safeStringList(node, "cons"),
                    JsonHelpers.safeStringList(node, "suggestions"),
                    normalizedJobDescription,
                    jdInsights.getExperienceLevel(),
                    jdInsights.getSkills(),
                    jdInsights.getTechnologies(),
                    jdInsights.getKeywords(),
                    jdInsights.getResponsibilities(),
                    jdInsights.getQualifications(),
                    jdInsights.getLocation()
            );

            try {
                reportClient.save(internalApiKey, saveRequest);
            } catch (Exception e) {
                log.error("report-service save failed (job {}): {}", jobId, e.getMessage());
                updateJob(jobId, AnalysisStatus.FAILED, 0, null,
                        "Your analysis finished but we couldn't save the report. Please try again.");
                return;
            }

            updateJob(jobId, AnalysisStatus.COMPLETED, 100, "Analysis complete", null);

        } catch (Exception e) {
            log.error("Unexpected error during async analysis (job {}): {}", jobId, e.getMessage(), e);
            updateJob(jobId, AnalysisStatus.FAILED, 0, null,
                    "Something went wrong while analysing your resume. Please try again.");
        }
    }

    private void updateJob(String jobId, AnalysisStatus status, int progress, String message, String errorMessage) {
        analysisJobRepo.findById(jobId).ifPresentOrElse(job -> {
            job.setStatus(status);
            job.setProgress(progress);
            job.setMessage(message);
            job.setErrorMessage(errorMessage);
            job.setUpdatedAt(Instant.now());
            analysisJobRepo.save(job);
        }, () -> log.error("AnalysisJob {} disappeared mid-processing - could not persist status {}", jobId, status));
    }
}
