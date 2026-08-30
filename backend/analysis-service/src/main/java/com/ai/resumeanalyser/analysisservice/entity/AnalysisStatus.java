package com.ai.resumeanalyser.analysisservice.entity;

public enum AnalysisStatus {
    QUEUED,
    PARSING_RESUME,
    ANALYZING_WITH_AI,
    PROCESSING_RESULT,
    SAVING_REPORT,
    COMPLETED,
    FAILED
}
