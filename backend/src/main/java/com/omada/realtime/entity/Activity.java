package com.omada.realtime.entity;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.omada.realtime.security.IdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;
@Document(collection="activities")
@CompoundIndex(name = "ws_created_idx", def = "{'workspaceId': 1, 'createdAt': -1}")
@CompoundIndex(name = "board_created_idx", def = "{'boardId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id
    private UUID id;
    private String workspaceId;
    private String boardId;
    private String actorId;
    private String type;
    private String entityId;
    private Map<String,Object> metadata;
    private Instant createdAt;
    }
