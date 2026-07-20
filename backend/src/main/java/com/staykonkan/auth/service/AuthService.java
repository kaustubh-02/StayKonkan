package com.staykonkan.auth.service;

import com.staykonkan.auth.dto.AuthResponse;
import com.staykonkan.auth.dto.LoginRequest;
import com.staykonkan.auth.dto.RefreshTokenRequest;
import com.staykonkan.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);
}
