package com.omada.realtime.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Workspace-level membership: a user's role within a single workspace.
 * Roles: OWNER > ADMIN > MEMBER > VIEWER.
 * IDs are generated via IdGenerator.uuid() in the service layer.
 */
@Document(collection = "workspace_memberships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMembership {
    @Id
    private UUID id;

    @NotNull
    private String userId;

    @NotNull
    private String workspaceId;

    @NotNull
    private String role; // OWNER, ADMIN, MEMBER or VIEWER

    @NotNull
    private Instant joinedAt;
}
