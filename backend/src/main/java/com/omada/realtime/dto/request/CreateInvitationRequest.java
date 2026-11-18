package com.omada.realtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Create a workspace invitation: invitee email and role (optional target board). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvitationRequest {
    @NotBlank
    public String email;

    @NotBlank
    public String role; // OWNER, ADMIN, MEMBER or VIEWER — validated in the service layer.

    public String boardId; // optional target board within the workspace
}
