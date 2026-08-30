package com.ai.resumeanalyser.reportservice.controller;

import com.ai.resumeanalyser.reportservice.dto.SaveReportRequest;
import com.ai.resumeanalyser.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportController {

    private final ReportService reportService;

    @Value("${internal.api-key:}")
    private String expectedKey;

    private boolean keyValid(String provided) {
        return expectedKey != null && !expectedKey.isBlank() && expectedKey.equals(provided);
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestHeader("X-Internal-Api-Key") String providedKey,
                                   @RequestBody SaveReportRequest request) {
        if (!keyValid(providedKey)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        reportService.saveReport(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/by-email/{email}")
    public ResponseEntity<?> deleteByEmail(@RequestHeader("X-Internal-Api-Key") String providedKey,
                                            @PathVariable("email") String email) {
        if (!keyValid(providedKey)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        reportService.deleteByEmail(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
