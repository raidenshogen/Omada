package com.omada.realtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Add a member to an organization (user resolved by email). */
@Data @NoArgsConstructor @AllArgsConstructor
public class AddMembershipRequest {
    @NotBlank public String email;
    /** OWNER, ADMIN, MEMBER or VIEWER — validated in the service layer. */
    @NotBlank public String role;
}
