package com.ai.resumeanalyser.reportservice.controller;

import com.ai.resumeanalyser.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        return reportService.getLatestReport();
    }
}
