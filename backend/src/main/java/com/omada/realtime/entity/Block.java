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

/**
 * A single block of a collaborative document. Blocks are stored individually
 * (not embedded) so a stale editor only conflicts with one block, not the
 * whole document — the "simple OT" version field is per block.
 */
@Document(collection="blocks")
@CompoundIndex(name = "doc_pos_idx", def = "{'documentId': 1, 'position': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {
    @Id private UUID id;
    private String documentId;
    /** paragraph | heading | todo | list — see BlockType. */
    private String type;
    private String text;
    private boolean checked;
    private int position;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
