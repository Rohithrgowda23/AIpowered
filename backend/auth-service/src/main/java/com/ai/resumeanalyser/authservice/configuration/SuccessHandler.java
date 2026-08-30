package com.ai.resumeanalyser.authservice.configuration;

import com.ai.resumeanalyser.authservice.entity.UsersTable;
import com.ai.resumeanalyser.authservice.repository.UsersTableRepo;
import com.ai.resumeanalyser.authservice.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SuccessHandler implements AuthenticationSuccessHandler {

    private final UsersTableRepo usersTableRepository;
    private final JwtService jwtService;

    @Value("${frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> userData = oAuth2User.getAttributes();

        String email = userData.get("email").toString();
        String name = userData.get("name").toString();

        if (!usersTableRepository.existsById(email)) {
            UsersTable newUser = UsersTable.builder()
                    .username(name)
                    .email(email)
                    .password("")
                    .previousResults(false)
                    .resetOtp(null)
                    .resetExpiration(null)
                    .build();
            usersTableRepository.save(newUser);
        }

        String token = jwtService.generateToken(email);
        response.sendRedirect(frontendBaseUrl + "/?token=" + token);
    }
}
