package com.ai.resumeanalyser.authservice.repository;

import com.ai.resumeanalyser.authservice.entity.UsersTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersTableRepo extends JpaRepository<UsersTable, String> {
}
