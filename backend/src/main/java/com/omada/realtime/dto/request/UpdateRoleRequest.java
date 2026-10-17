package com.omada.realtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** Change a member's organization role. */
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateRoleRequest {
    /** OWNER, ADMIN, MEMBER or VIEWER — validated in the service layer. */
    @NotBlank public String role;
}
