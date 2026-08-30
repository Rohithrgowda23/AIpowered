package com.ai.resumeanalyser.resumeservice.controller;

import com.ai.resumeanalyser.resumeservice.dto.ResumeParseResponse;
import com.ai.resumeanalyser.resumeservice.service.ResumeParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeParsingService resumeParsingService;

    @PostMapping
    public ResponseEntity<?> uploadAndParse(@RequestParam("file") MultipartFile file) {
        try {
            ResumeParseResponse result = resumeParsingService.parse(file);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to read resume file", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
