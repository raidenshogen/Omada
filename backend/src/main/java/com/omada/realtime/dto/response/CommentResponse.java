package com.omada.realtime.dto.response;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor public class CommentResponse { public String id; public String body; public String authorId; }