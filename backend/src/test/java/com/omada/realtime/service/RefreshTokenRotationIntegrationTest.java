package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.RefreshRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.AuthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for refresh token rotation: old refresh token is revoked
 * on use (rotation), new refresh token works, revoked token is rejected.
 */
public class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void refresh_rotatesRefreshToken_oldTokenRevoked() {
        // given: a registered user with access + refresh tokens
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("rotate@example.com");
        reg.setName("rotate");
        reg.setPassword("password123");
        AuthResponse first = userService.register(reg);

        // when: refreshing with the original refresh token
        RefreshRequest refreshReq = new RefreshRequest();
        refreshReq.refreshToken = first.refreshToken;
        AuthResponse rotated = userService.refresh(refreshReq);

        // then: new access and refresh tokens are issued
        assertThat(rotated.accessToken).isNotEqualTo(first.accessToken);
        assertThat(rotated.refreshToken).isNotEqualTo(first.refreshToken);

        // and: the old refresh token is revoked (rotation)
        refreshReq.refreshToken = first.refreshToken;
        assertThatThrownBy(() -> userService.refresh(refreshReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");

        // and: the new refresh token works for another rotation
        refreshReq.refreshToken = rotated.refreshToken;
        AuthResponse again = userService.refresh(refreshReq);
        assertThat(again.accessToken).isNotEqualTo(rotated.accessToken);
    }

    @Test
    void refresh_unknownOrExpiredToken_rejected() {
        // given: a random token that was never issued
        RefreshRequest refreshReq = new RefreshRequest();
        refreshReq.refreshToken = "never-issued-token";

        // then: refresh is rejected
        assertThatThrownBy(() -> userService.refresh(refreshReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
