package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.ResumeParseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Component
public class ResumeServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceClient.class);

    // A sleeping free-tier Render service needs about a minute to wake up.
    private static final long MAX_WAIT_MS = 150_000L;
    private static final long RETRY_DELAY_MS = 10_000L;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ResumeServiceClient(@Value("${resume-service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public ResumeParseResponse uploadAndParse(String bearerToken, String fileName, byte[] fileBytes) {
        String url = baseUrl + "/api/resumes";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;

        for (int attempt = 1; ; attempt++) {
            try {
                var response = restTemplate.postForEntity(url, requestEntity, ResumeParseResponse.class);
                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    throw new RestClientException("resume-service returned " + response.getStatusCode());
                }
                return response.getBody();
            } catch (RestClientException e) {
                if (!isTransient(e) || System.currentTimeMillis() + RETRY_DELAY_MS >= deadline) {
                    throw e;
                }
                log.warn("resume-service not ready (attempt {}): {} - retrying in {}s",
                        attempt, e.getMessage(), RETRY_DELAY_MS / 1000);
                pause();
            }
        }
    }

    // 429/502/503/504 or a connection problem usually means the service is still waking up.
    private static boolean isTransient(RestClientException e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e instanceof HttpStatusCodeException statusException) {
            int status = statusException.getStatusCode().value();
            return status == 429 || status == 502 || status == 503 || status == 504;
        }
        return false;
    }

    private static void pause() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Interrupted while waiting for resume-service");
        }
    }
}