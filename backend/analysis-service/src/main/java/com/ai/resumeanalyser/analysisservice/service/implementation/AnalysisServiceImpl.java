package com.ai.resumeanalyser.analysisservice.service.implementation;

import com.ai.resumeanalyser.analysisservice.dto.AnalysisStartResponse;
import com.ai.resumeanalyser.analysisservice.dto.AnalysisStatusDto;
import com.ai.resumeanalyser.analysisservice.entity.AnalysisJob;
import com.ai.resumeanalyser.analysisservice.entity.AnalysisStatus;
import com.ai.resumeanalyser.analysisservice.repository.AnalysisJobRepo;
import com.ai.resumeanalyser.analysisservice.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisJobRepo analysisJobRepo;
    private final AnalysisWorker analysisWorker;

    private String normalizeRoles(String roles) {
        return roles == null ? "" : roles.trim();
    }

    private String normalizeJobDescription(String jobDescription) {
        if (jobDescription == null) return "";
        String trimmed = jobDescription.trim();
        return trimmed.length() > 6000 ? trimmed.substring(0, 6000) : trimmed;
    }

    @Override
    public ResponseEntity<?> startAnalysis(String bearerToken, String roles, String jobDescription, MultipartFile file) throws IOException {

        String normalizedRoles = normalizeRoles(roles);
        if (normalizedRoles.isEmpty()) {
            return new ResponseEntity<>("Role must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            return new ResponseEntity<>("Please upload the resume", HttpStatus.BAD_REQUEST);
        }

        String normalizedJobDescription = normalizeJobDescription(jobDescription);
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename();

        String uname = SecurityContextHolder.getContext().getAuthentication().getName();

        Instant now = Instant.now();
        AnalysisJob job = new AnalysisJob(
                UUID.randomUUID().toString(), uname, AnalysisStatus.QUEUED, 0, "Queued for analysis", null, now, now);
        analysisJobRepo.save(job);

        analysisWorker.processAnalysisAsync(job.getId(), uname, bearerToken, normalizedRoles, normalizedJobDescription, fileName, fileBytes);

        return new ResponseEntity<>(new AnalysisStartResponse(job.getId(), job.getStatus()), HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<?> getAnalysisStatus(String analysisId) {
        AnalysisJob job = analysisJobRepo.findById(analysisId).orElse(null);
        String uname = SecurityContextHolder.getContext().getAuthentication().getName();

        if (job == null || !job.getEmail().equals(uname)) {
            return new ResponseEntity<>("Analysis not found", HttpStatus.NOT_FOUND);
        }

        boolean reportReady = job.getStatus() == AnalysisStatus.COMPLETED;
        AnalysisStatusDto dto = new AnalysisStatusDto(
                job.getId(), job.getStatus(), job.getProgress(), job.getMessage(), job.getErrorMessage(), reportReady);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
