package com.omada.realtime.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommentResponse {
    public String id;
    public String body;
    public String authorId;
    public String authorName;

    public CommentResponse(String id, String body, String authorId) {
        this(id, body, authorId, null);
    }

    public CommentResponse(String id, String body, String authorId, String authorName) {
        this.id = id;
        this.body = body;
        this.authorId = authorId;
        this.authorName = authorName;
    }
}