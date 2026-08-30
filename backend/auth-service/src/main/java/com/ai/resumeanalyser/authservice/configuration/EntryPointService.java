package com.ai.resumeanalyser.authservice.configuration;

import com.ai.resumeanalyser.authservice.entity.UsersTable;
import com.ai.resumeanalyser.authservice.repository.UsersTableRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntryPointService implements UserDetailsService {

    private final UsersTableRepo usersTableRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsersTable user = usersTableRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .roles("USER")
                .build();
    }
}
