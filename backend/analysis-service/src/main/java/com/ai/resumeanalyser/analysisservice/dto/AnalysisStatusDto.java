package com.ai.resumeanalyser.analysisservice.dto;

import com.ai.resumeanalyser.analysisservice.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisStatusDto {
    private String analysisId;
    private AnalysisStatus status;
    private int progress;
    private String message;
    private String errorMessage;
    private boolean reportReady;
}
