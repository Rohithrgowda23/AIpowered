package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.ResumeParseResponse;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Component
public class ResumeServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ResumeServiceClient(@Value("${resume-service.base-url}") String baseUrl) {
        // Generous read timeout: a sleeping Render service needs time to wake up.
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

        var response = restTemplate.postForEntity(url, requestEntity, ResumeParseResponse.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RestClientException("resume-service returned " + response.getStatusCode());
        }
        return response.getBody();
    }
}