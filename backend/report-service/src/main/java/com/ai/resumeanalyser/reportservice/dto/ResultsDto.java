package com.ai.resumeanalyser.reportservice.dto;

import com.ai.resumeanalyser.reportservice.client.JobDto;

import java.util.List;

public record ResultsDto(
        int score,
        int atsoptimizationscore,
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
        List<JobDto> jobs,
        String jobRole,
        String jobDescription,
        String jdExperienceLevel,
        List<String> jdSkills,
        List<String> jdTechnologies,
        List<String> jdKeywords,
        List<String> jdResponsibilities,
        List<String> jdQualifications,
        String jdLocation,
        String jobSearchMessage
) {
}
