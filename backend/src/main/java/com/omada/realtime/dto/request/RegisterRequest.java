package com.omada.realtime.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank public String email;
    @NotBlank public String name;
    @NotBlank public String password; }