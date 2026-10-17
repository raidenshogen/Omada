package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * Refresh token request DTO.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class RefreshRequest {
    public String refreshToken;
}