package com.omada.realtime.entity;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document(collection="comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id private UUID id;
    private UUID cardId;
    private UUID authorId;
    private String authorName;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;
}
