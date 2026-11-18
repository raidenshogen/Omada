package com.omada.realtime.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {
    @NotBlank public String name;
    @Email @NotBlank public String email;
}
