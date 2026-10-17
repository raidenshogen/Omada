package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.Instant;

/**
 * A collaborative document: a titled container owned by a workspace (and
 * optionally a board) whose content is an ordered set of {@link Block}s.
 *
 * The Mongo {@code @Document} annotation is fully-qualified here because this
 * class is itself named {@code Document} — importing the annotation would
 * shadow the class name.
 */
@org.springframework.data.mongodb.core.mapping.Document(collection="documents")
@CompoundIndex(name = "doc_ws_idx", def = "{'workspaceId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id private UUID id;
    private String workspaceId;
    private String boardId;
    private String title;
    private String createdBy;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
