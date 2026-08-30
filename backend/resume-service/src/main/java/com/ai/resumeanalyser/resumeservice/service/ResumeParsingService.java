package com.ai.resumeanalyser.resumeservice.service;

import com.ai.resumeanalyser.resumeservice.dto.ResumeParseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeParsingService {
    ResumeParseResponse parse(MultipartFile file) throws Exception;
}
