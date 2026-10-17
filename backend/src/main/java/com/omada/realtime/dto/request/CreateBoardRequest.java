package com.omada.realtime.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateBoardRequest {
    @NotBlank
    public String workspaceId;
    @NotBlank
    public String title;
    public String description; }