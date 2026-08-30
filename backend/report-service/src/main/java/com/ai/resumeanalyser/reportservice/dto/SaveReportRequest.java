package com.ai.resumeanalyser.reportservice.dto;

import java.util.List;

public record SaveReportRequest(
        String email,
        int score,
        int atsoptimizationscore,
        String roles,
        String summary,
        String experienceLevel,
        List<String> skills,
        List<String> missingSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> interviewTips,
        List<String> pros,
        List<String> cons,
        List<String> suggestions,
        String jobDescription,
        String jdExperienceLevel,
        List<String> jdSkills,
        List<String> jdTechnologies,
        List<String> jdKeywords,
        List<String> jdResponsibilities,
        List<String> jdQualifications,
        String jdLocation
) {
}
