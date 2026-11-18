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
 * Workspace invitation: an email-based invite to join a single workspace with a role.
 * Token is a UUIDv4, expires 24h after creation.
 * Status: PENDING, ACCEPTED, DECLINED, EXPIRED or REVOKED.
 * IDs and tokens are generated via IdGenerator/UUID.randomUUID() in the service layer.
 */
@Document(collection = "invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {
    @Id
    private UUID id;

    @NotNull
    private UUID token; // UUIDv4 invitation token

    @NotNull
    private String email;

    @NotNull
    private String workspaceId;

    private String boardId; // optional target board within the workspace

    @NotNull
    private String role; // OWNER, ADMIN, MEMBER or VIEWER

    @NotNull
    private String status; // PENDING, ACCEPTED, DECLINED or EXPIRED

    @NotNull
    private Instant expiresAt; // 24h from creation
}
