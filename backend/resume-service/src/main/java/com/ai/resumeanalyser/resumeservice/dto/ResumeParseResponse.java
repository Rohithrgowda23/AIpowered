package com.ai.resumeanalyser.resumeservice.dto;

public record ResumeParseResponse(String resumeId, String fileName, String extractedText, String status) {
}
