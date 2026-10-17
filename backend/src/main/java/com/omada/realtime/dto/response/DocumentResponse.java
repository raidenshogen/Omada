package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
@Data @NoArgsConstructor @AllArgsConstructor public class DocumentResponse {
    public String id;
    public String workspaceId;
    public String boardId;
    public String title;
    public String createdBy;
    public int version;
    public Instant createdAt;
    public Instant updatedAt;
}
