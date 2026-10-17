package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.LoginRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.request.RefreshRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.dto.response.UserResponse;
import com.omada.realtime.entity.User;
import com.omada.realtime.mapper.UserMapper;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.service.UserService;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * User service implementation with BCrypt password hashing
 * and JWT access/refresh token issuance.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = userMapper.toEntity(req);
        user.setId(IdGenerator.uuid());
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        UserResponse userResponse = userMapper.toResponse(user);
        AuthResponse resp = new AuthResponse();
        resp.accessToken = JwtUtil.generateAccess(user.getId().toString());
        resp.refreshToken = JwtUtil.generateRefresh(user.getId().toString());
        resp.user = userResponse;
        return resp;
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email);
        if (user == null || !passwordEncoder.matches(req.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        UserResponse userResponse = userMapper.toResponse(user);
        AuthResponse resp = new AuthResponse();
        resp.accessToken = JwtUtil.generateAccess(user.getId().toString());
        resp.refreshToken = JwtUtil.generateRefresh(user.getId().toString());
        resp.user = userResponse;
        return resp;
    }

    @Override
    public AuthResponse refresh(RefreshRequest req) {
        String subject = JwtUtil.validate(req.refreshToken);
        if (subject == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        AuthResponse resp = new AuthResponse();
        resp.accessToken = JwtUtil.generateAccess(subject);
        resp.refreshToken = JwtUtil.generateRefresh(subject);
        return resp;
    }

    @Override
    public UserResponse me(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userMapper.toResponse(user);
    }
}
