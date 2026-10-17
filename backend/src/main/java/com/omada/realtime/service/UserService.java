package com.omada.realtime.service;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.request.LoginRequest;
import com.omada.realtime.dto.request.RefreshRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.dto.response.UserResponse;

public interface UserService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refresh(RefreshRequest req);
    UserResponse me(String userId);
}