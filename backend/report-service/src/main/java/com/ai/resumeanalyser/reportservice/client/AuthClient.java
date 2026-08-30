package com.ai.resumeanalyser.reportservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/internal/users/{email}/previous-results")
    void markHasPreviousResults(@RequestHeader("X-Internal-Api-Key") String internalApiKey,
                                @PathVariable("email") String email);
}