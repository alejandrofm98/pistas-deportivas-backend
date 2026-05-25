package com.sportreserve.admin;

import com.sportreserve.admin.dto.LoginRequest;
import com.sportreserve.admin.dto.LoginResponse;
import com.sportreserve.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final String adminUsername;
    private final String adminPassword;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthService(
        @Value("${app.admin.username}") String adminUsername,
        @Value("${app.admin.password}") String adminPassword,
        JwtTokenProvider jwtTokenProvider) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.username()) || !adminPassword.equals(request.password())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(request.username());
        return new LoginResponse(token, request.username(), "ADMIN");
    }
}
