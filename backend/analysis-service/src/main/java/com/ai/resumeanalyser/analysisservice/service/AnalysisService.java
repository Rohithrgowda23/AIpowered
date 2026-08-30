package com.ai.resumeanalyser.analysisservice.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AnalysisService {
    ResponseEntity<?> startAnalysis(String bearerToken, String roles, String jobDescription, MultipartFile file) throws IOException;
    ResponseEntity<?> getAnalysisStatus(String analysisId);
}
