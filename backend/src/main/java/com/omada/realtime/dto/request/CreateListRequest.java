package com.omada.realtime.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateListRequest {
    // boardId is set by the controller from the @PathVariable, not from the client
    public String boardId;
    @NotBlank
    public String title;
    public int position; }