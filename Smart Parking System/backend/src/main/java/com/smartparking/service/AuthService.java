package com.smartparking.service;

import com.smartparking.dto.request.LoginRequest;
import com.smartparking.dto.request.RefreshTokenRequest;
import com.smartparking.dto.request.RegisterRequest;
import com.smartparking.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
}
