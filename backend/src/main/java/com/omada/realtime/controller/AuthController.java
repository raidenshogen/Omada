package com.omada.realtime.controller;

import com.omada.realtime.dto.request.LoginRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.request.RefreshRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.dto.response.UserResponse;
import com.omada.realtime.security.RefreshTokenService;
import com.omada.realtime.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for register, login, refresh and me.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    public AuthController(UserService userService,
                          RefreshTokenService refreshTokenService){
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req){
        return userService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req){
        return userService.login(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req){
        return userService.refresh(req);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest req){
        refreshTokenService.revoke(req.refreshToken);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication){
        String userId = authentication != null ? authentication.getName() : null;
        // Fallback: for demo, return empty if no auth
        return userService.me(userId != null ? userId : "");
    }
}
