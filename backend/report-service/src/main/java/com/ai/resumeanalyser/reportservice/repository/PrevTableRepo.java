package com.ai.resumeanalyser.reportservice.repository;

import com.ai.resumeanalyser.reportservice.entity.PreviousTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrevTableRepo extends JpaRepository<PreviousTable, String> {
}
