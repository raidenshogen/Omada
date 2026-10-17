package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.LoginRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.entity.User;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for UserService against real MongoDB + JWT utilities.
 */
public class UserServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerUser_savesToMongo_andReturnsJwt() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setName("Alice");
        req.setPassword("password123");

        AuthResponse resp = userService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getUser()).isNotNull();
        assertThat(resp.getUser().getEmail()).isEqualTo("alice@example.com");
        // real JWTs, not the old "token" stub
        assertThat(resp.getAccessToken()).isNotBlank().isNotEqualTo("token");
        assertThat(resp.getRefreshToken()).isNotBlank();

        User saved = userRepository.findByEmail("alice@example.com");
        assertThat(saved).isNotNull();
        // password is hashed, never stored in clear
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
    }

    @Test
    void login_withCorrectPassword_returnsToken() {
        RegisterRequest register = new RegisterRequest();
        register.setEmail("bob@example.com");
        register.setName("Bob");
        register.setPassword("secret123");
        userService.register(register);

        LoginRequest login = new LoginRequest();
        login.setEmail("bob@example.com");
        login.setPassword("secret123");

        AuthResponse resp = userService.login(login);
        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isNotBlank();
        assertThat(resp.getUser().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void login_withWrongPassword_throws() {
        RegisterRequest register = new RegisterRequest();
        register.setEmail("carol@example.com");
        register.setName("Carol");
        register.setPassword("right-password");
        userService.register(register);

        LoginRequest login = new LoginRequest();
        login.setEmail("carol@example.com");
        login.setPassword("wrong-password");

        assertThatThrownBy(() -> userService.login(login))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest first = new RegisterRequest();
        first.setEmail("dave@example.com");
        first.setName("Dave");
        first.setPassword("password123");
        userService.register(first);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setEmail("dave@example.com");
        duplicate.setName("Dave Again");
        duplicate.setPassword("password123");

        assertThatThrownBy(() -> userService.register(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }
}
