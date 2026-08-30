package com.ai.resumeanalyser.reportservice.client;

import java.util.List;

public record JobSearchRequest(
        String role,
        List<String> jdSkills,
        List<String> jdTechnologies,
        List<String> jdKeywords,
        String jdLocation
) {
}
