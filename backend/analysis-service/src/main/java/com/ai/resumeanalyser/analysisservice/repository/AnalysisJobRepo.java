package com.ai.resumeanalyser.analysisservice.repository;

import com.ai.resumeanalyser.analysisservice.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepo extends JpaRepository<AnalysisJob, String> {
}
