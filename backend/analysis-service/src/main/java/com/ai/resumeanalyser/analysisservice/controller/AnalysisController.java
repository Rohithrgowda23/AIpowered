package com.ai.resumeanalyser.analysisservice.controller;

import com.ai.resumeanalyser.analysisservice.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<?> startAnalysis(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @RequestParam("roles") String roles,
            @RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription,
            @RequestParam("file") MultipartFile file) throws IOException {

        return analysisService.startAnalysis(bearerToken, roles, jobDescription, file);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getStatus(@PathVariable("id") String id) {
        return analysisService.getAnalysisStatus(id);
    }
}
