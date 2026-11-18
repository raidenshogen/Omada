package com.omada.realtime.controller;

import com.omada.realtime.dto.request.LoginRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.request.RefreshRequest;
import com.omada.realtime.dto.request.UpdateProfileRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.dto.response.UserResponse;
import com.omada.realtime.security.RefreshTokenService;
import com.omada.realtime.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    public AuthController(UserService userService,
                          RefreshTokenService refreshTokenService){
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req){
        AuthResponse resp = userService.register(req);
        log.info("AUTH_REGISTER email={} userId={}", req.email, resp.getUser() != null ? resp.getUser().getId() : "unknown");
        return resp;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req){
        AuthResponse resp = userService.login(req);
        log.info("AUTH_LOGIN email={} userId={}", req.email, resp.getUser() != null ? resp.getUser().getId() : "unknown");
        return resp;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req){
        AuthResponse resp = userService.refresh(req);
        log.info("AUTH_REFRESH userId={}", resp.getUser() != null ? resp.getUser().getId() : "unknown");
        return resp;
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest req){
        refreshTokenService.revoke(req.refreshToken);
        log.info("AUTH_LOGOUT tokenRevoked");
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication){
        String userId = authentication != null ? authentication.getName() : null;
        return userService.me(userId != null ? userId : "");
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest req, Authentication authentication){
        String userId = authentication.getName();
        UserResponse resp = userService.updateProfile(userId, req);
        log.info("AUTH_PROFILE_UPDATE userId={}", userId);
        return resp;
    }
}
