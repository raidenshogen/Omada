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
 * Board-level membership: a user's membership within a single board.
 * IDs are generated via IdGenerator.uuid() in the service layer.
 */
@Document(collection = "board_memberships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMembership {
    @Id
    private UUID id;

    @NotNull
    private String userId;

    @NotNull
    private String boardId;

    @NotNull
    private Instant joinedAt;
}
