package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor public class AuthResponse { public String accessToken; public String refreshToken; public UserResponse user; }