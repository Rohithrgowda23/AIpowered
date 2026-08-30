package com.ai.resumeanalyser.analysisservice.client;

import com.ai.resumeanalyser.analysisservice.dto.ResumeParseResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Component
public class ResumeServiceClient {

    private final RestTemplate restTemplate;
    private final org.springframework.cloud.client.loadbalancer.LoadBalancerClient loadBalancerClient;

    public ResumeServiceClient(org.springframework.cloud.client.loadbalancer.LoadBalancerClient loadBalancerClient) {
        this.restTemplate = new RestTemplate();
        this.loadBalancerClient = loadBalancerClient;
    }

    public ResumeParseResponse uploadAndParse(String bearerToken, String fileName, byte[] fileBytes) {
        var serviceInstance = loadBalancerClient.choose("RESUME-SERVICE");
        if (serviceInstance == null) {
            throw new RestClientException("resume-service is not available in the registry (Eureka)");
        }
        String url = serviceInstance.getUri() + "/api/resumes";

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
