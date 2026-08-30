package com.ai.resumeanalyser.authservice.controller;

import com.ai.resumeanalyser.authservice.entity.UsersTable;
import com.ai.resumeanalyser.authservice.repository.UsersTableRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UsersTableRepo usersTableRepository;

    @Value("${internal.api-key:}")
    private String expectedKey;

    @PostMapping("/{email}/previous-results")
    public ResponseEntity<?> markHasPreviousResults(
            @RequestHeader("X-Internal-Api-Key") String providedKey,
            @PathVariable("email") String email) {

        if (expectedKey == null || expectedKey.isBlank() || !expectedKey.equals(providedKey)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        UsersTable user = usersTableRepository.findById(email).orElse(null);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        user.setPreviousResults(true);
        usersTableRepository.save(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}