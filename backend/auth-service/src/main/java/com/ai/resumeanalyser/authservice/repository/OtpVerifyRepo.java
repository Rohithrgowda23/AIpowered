package com.ai.resumeanalyser.authservice.repository;

import com.ai.resumeanalyser.authservice.entity.OtpVerify;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpVerifyRepo extends JpaRepository<OtpVerify, String> {
}
