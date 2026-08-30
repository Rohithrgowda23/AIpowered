package com.ai.resumeanalyser.reportservice.service;

import com.ai.resumeanalyser.reportservice.dto.SaveReportRequest;
import org.springframework.http.ResponseEntity;

public interface ReportService {
    ResponseEntity<?> getLatestReport();
    void saveReport(SaveReportRequest request);
    void deleteByEmail(String email);
}
