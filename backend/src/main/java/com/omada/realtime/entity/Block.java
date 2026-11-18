package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="blocks")
@CompoundIndex(name = "doc_pos_idx", def = "{'documentId': 1, 'position': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {
    @Id private UUID id;
    private String documentId;
    private String type;
    private String text;
    private boolean checked;
    private int position;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
