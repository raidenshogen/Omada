package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
@Data @NoArgsConstructor @AllArgsConstructor public class ActivityResponse {
    public String id;
    public String type;
    public String entityId;
    public String actorId;
    public String workspaceId;
    public String boardId;
    public Instant createdAt;
}
