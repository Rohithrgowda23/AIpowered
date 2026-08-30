package com.ai.resumeanalyser.analysisservice.dto;

public record ResumeParseResponse(String resumeId, String fileName, String extractedText, String status) {
}
