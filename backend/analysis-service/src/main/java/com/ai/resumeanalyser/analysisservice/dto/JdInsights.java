package com.ai.resumeanalyser.analysisservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdInsights {
    private String experienceLevel;
    private List<String> skills;
    private List<String> technologies;
    private List<String> keywords;
    private List<String> responsibilities;
    private List<String> qualifications;
    private String location;
}
