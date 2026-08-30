package com.ai.resumeanalyser.resumeservice.service.implementation;

import com.ai.resumeanalyser.resumeservice.dto.ResumeParseResponse;
import com.ai.resumeanalyser.resumeservice.service.ResumeParsingService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_BYTES = 3L * 1024 * 1024;

    @Override
    public ResumeParseResponse parse(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload the resume");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Upload a resume in pdf/doc format");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Upload a file less than 3MB");
        }

        Tika tika = new Tika();
        String extractedText = tika.parseToString(new ByteArrayInputStream(file.getBytes()));

        return new ResumeParseResponse(
                UUID.randomUUID().toString(),
                file.getOriginalFilename(),
                extractedText,
                "PARSED"
        );
    }
}
