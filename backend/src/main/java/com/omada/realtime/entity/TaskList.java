package com.omada.realtime.entity;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.omada.realtime.security.IdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document(collection="lists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskList {
    // IDs are generated via IdGenerator.uuid() in service layer
    @Id private UUID id;
    @Indexed
    private String boardId;
    private String title;
    private int position;
    private Instant createdAt;
  }
