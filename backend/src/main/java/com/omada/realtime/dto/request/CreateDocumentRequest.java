package com.omada.realtime.dto.request;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateDocumentRequest {
    public String title;
    public String boardId;
}
